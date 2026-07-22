package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.WhatsappAudienceType;
import in.jlenterprises.ecommerce.constant.WhatsappCampaignStatus;
import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;
import in.jlenterprises.ecommerce.dto.admin.BroadcastResult;
import in.jlenterprises.ecommerce.dto.whatsapp.AudienceCustomerDto;
import in.jlenterprises.ecommerce.dto.whatsapp.AudiencePreviewDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignAnalyticsDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignDetailDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignDto;
import in.jlenterprises.ecommerce.dto.whatsapp.DeliveryLogDto;
import in.jlenterprises.ecommerce.dto.whatsapp.MessageLogDto;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.entity.WhatsappCampaign;
import in.jlenterprises.ecommerce.entity.WhatsappMessageLog;
import in.jlenterprises.ecommerce.entity.WhatsappTemplate;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.notification.WhatsAppService;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.repository.WhatsappCampaignRepository;
import in.jlenterprises.ecommerce.repository.WhatsappMessageLogRepository;
import in.jlenterprises.ecommerce.repository.WhatsappTemplateRepository;
import in.jlenterprises.ecommerce.request.whatsapp.CampaignRequest;
import in.jlenterprises.ecommerce.service.WhatsappCampaignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WhatsappCampaignServiceImpl implements WhatsappCampaignService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappCampaignServiceImpl.class);

    private final WhatsappCampaignRepository campaignRepo;
    private final WhatsappMessageLogRepository logRepo;
    private final WhatsappTemplateRepository templateRepo;
    private final UserRepository userRepo;
    private final WhatsAppService whatsApp;
    /** Self proxy (lazy to avoid a construction cycle) so the @Async worker call goes through the proxy. */
    private final WhatsappCampaignService self;

    public WhatsappCampaignServiceImpl(WhatsappCampaignRepository campaignRepo,
                                       WhatsappMessageLogRepository logRepo,
                                       WhatsappTemplateRepository templateRepo,
                                       UserRepository userRepo,
                                       WhatsAppService whatsApp,
                                       @Lazy WhatsappCampaignService self) {
        this.campaignRepo = campaignRepo;
        this.logRepo = logRepo;
        this.templateRepo = templateRepo;
        this.userRepo = userRepo;
        this.whatsApp = whatsApp;
        this.self = self;
    }

    // ── Audience ──
    private List<User> resolveAudience(WhatsappAudienceType type) {
        return switch (type) {
            case ALL_OPTED_IN -> userRepo.findByWhatsappOptInTrueAndPhoneNotNull();
            case VERIFIED_OPTED_IN -> userRepo.findByWhatsappOptInTrueAndPhoneVerifiedTrueAndPhoneNotNull();
            case HAS_ORDERED -> userRepo.findOptedInWithOrders();
            case EVERYONE_WITH_PHONE -> userRepo.findByPhoneNotNull();
            case MANUAL -> List.of(); // manual recipients come from the campaign, not the type — see resolveRecipients
        };
    }

    /** The actual recipients for a campaign: hand-picked ids for MANUAL, else the audience type. */
    private List<User> resolveRecipients(WhatsappCampaign c) {
        if (c.getAudienceType() == WhatsappAudienceType.MANUAL) {
            List<UUID> ids = parseIds(c.getManualRecipientIds());
            if (ids.isEmpty()) return List.of();
            return userRepo.findAllById(ids).stream()
                    .filter(u -> u.getPhone() != null && !u.getPhone().isBlank())
                    .toList();
        }
        return resolveAudience(c.getAudienceType());
    }

    private static List<UUID> parseIds(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        List<UUID> out = new ArrayList<>();
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                try { out.add(UUID.fromString(t)); } catch (IllegalArgumentException ignore) { /* skip bad id */ }
            }
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public AudiencePreviewDto previewAudience(WhatsappAudienceType audienceType, String city) {
        List<User> users = resolveAudience(audienceType == null ? WhatsappAudienceType.ALL_OPTED_IN : audienceType);
        List<String> sample = users.stream().limit(5).map(this::nameOf).toList();
        return new AudiencePreviewDto(users.size(), sample);
    }

    // ── CRUD ──
    @Override
    @Transactional(readOnly = true)
    public Page<CampaignDto> list(Pageable pageable) {
        return campaignRepo.findAllByOrderByCreatedAtDesc(pageable).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignDetailDto get(UUID id) {
        return toDetail(entity(id));
    }

    @Override
    @Transactional
    public CampaignDetailDto create(CampaignRequest r) {
        WhatsappCampaign c = new WhatsappCampaign();
        c.setName(r.name().trim());
        c.setAudienceType(r.audienceType());
        c.setCityFilter(r.cityFilter() == null || r.cityFilter().isBlank() ? null : r.cityFilter().trim());

        if (r.audienceType() == WhatsappAudienceType.MANUAL) {
            List<UUID> ids = r.recipientCustomerIds() == null ? List.of()
                    : r.recipientCustomerIds().stream().filter(java.util.Objects::nonNull).distinct().toList();
            if (ids.isEmpty()) {
                throw new BusinessException("Pick at least one recipient for a hand-picked broadcast.");
            }
            c.setManualRecipientIds(ids.stream().map(UUID::toString).collect(Collectors.joining(",")));
        }

        if (r.templateId() != null) {
            WhatsappTemplate t = templateRepo.findById(r.templateId())
                    .orElseThrow(() -> ResourceNotFoundException.of("WhatsappTemplate", r.templateId()));
            c.setTemplate(t);
            c.setBodyText(t.getBodyText());
        } else if (r.bodyText() != null && !r.bodyText().isBlank()) {
            c.setBodyText(r.bodyText().trim());
        } else {
            throw new BusinessException("Enter a message or pick a template.");
        }
        c.setCampaignStatus(WhatsappCampaignStatus.DRAFT);
        return toDetail(campaignRepo.save(c));
    }

    @Override
    @Transactional
    @Auditable(action = "SEND_WHATSAPP_CAMPAIGN", entity = "whatsapp_campaign")
    public CampaignDetailDto send(UUID id) {
        WhatsappCampaign c = entity(id);
        if (c.getCampaignStatus() != WhatsappCampaignStatus.DRAFT
                && c.getCampaignStatus() != WhatsappCampaignStatus.SCHEDULED) {
            throw new BusinessException("This campaign has already been sent.");
        }
        List<User> recipients = resolveRecipients(c);
        if (recipients.isEmpty()) {
            throw new BusinessException("No customers match this audience yet.");
        }
        // Live marketing to opted-in customers must use an approved template — Meta
        // rejects free text outside the 24h reply window. Demo mode simulates, so allow it.
        if (whatsApp.isConfigured() && !hasApprovedTemplate(c)) {
            throw new BusinessException("Live sending needs an approved template. Create a template with its "
                    + "Meta template name, then compose the campaign from it. (Free text only works inside the 24-hour reply window.)");
        }
        enqueue(c, recipients);
        // Hand the actual sending to a background worker once this tx commits — a large audience
        // must not block the request thread / one long DB transaction (Render free-tier timeouts).
        UUID cid = c.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { self.processQueued(cid); }
            });
        } else {
            self.processQueued(cid);
        }
        return toDetail(c);
    }

    /** Mark the campaign SENDING and persist one QUEUED message-log row per recipient. */
    private void enqueue(WhatsappCampaign c, List<User> recipients) {
        c.setDemoMode(!whatsApp.isConfigured());
        c.setCampaignStatus(WhatsappCampaignStatus.SENDING);
        c.setTotalRecipients(recipients.size());
        campaignRepo.save(c);
        for (User u : recipients) {
            WhatsappMessageLog msg = new WhatsappMessageLog();
            msg.setCampaign(c);
            msg.setUserId(u.getId());
            msg.setRecipientName(nameOf(u));
            msg.setPhone(u.getPhone());
            msg.setRenderedBody(render(c.getBodyText(), u.getFirstName()));
            msg.setMessageStatus(WhatsappMessageStatus.QUEUED);
            logRepo.save(msg);
        }
    }

    @Override
    @Async
    public void processQueued(UUID campaignId) {
        // Deliberately NOT transactional: one short transaction PER BATCH (via the self
        // proxy) instead of a single transaction — and its Hikari connection (pool max 5
        // on the free tier) — held across every HTTP send of the whole campaign. Progress
        // also commits as it happens, and a cancel mid-send takes effect between batches.
        while (self.processBatch(campaignId, 20) > 0) { /* next batch */ }
        self.finishCampaign(campaignId);
    }

    @Override
    @Transactional
    public int processBatch(UUID campaignId, int batchSize) {
        WhatsappCampaign c = campaignRepo.findById(campaignId).orElse(null);
        if (c == null || c.getCampaignStatus() == WhatsappCampaignStatus.CANCELLED) return 0;
        boolean demo = c.isDemoMode();
        List<WhatsappMessageLog> queued = logRepo
                .findByCampaign_IdAndMessageStatus(campaignId, WhatsappMessageStatus.QUEUED)
                .stream().limit(batchSize).toList();
        for (WhatsappMessageLog msg : queued) {
            sendOne(c, msg, demo);
            logRepo.save(msg);
        }
        return queued.size();
    }

    @Override
    @Transactional
    public void finishCampaign(UUID campaignId) {
        WhatsappCampaign c = campaignRepo.findById(campaignId).orElse(null);
        if (c == null) return;
        recount(c);
        c.setSentAt(Instant.now());
        campaignRepo.save(c);
    }

    @Override
    @Transactional
    @Auditable(action = "RETRY_WHATSAPP_CAMPAIGN", entity = "whatsapp_campaign")
    public CampaignDetailDto retryFailed(UUID id) {
        WhatsappCampaign c = entity(id);
        List<WhatsappMessageLog> failed = logRepo.findByCampaign_IdAndMessageStatus(id, WhatsappMessageStatus.FAILED);
        if (failed.isEmpty()) throw new BusinessException("No failed messages to retry.");
        boolean demo = !whatsApp.isConfigured();
        c.setDemoMode(demo);
        for (WhatsappMessageLog msg : failed) {
            sendOne(c, msg, demo);
        }
        logRepo.saveAll(failed);
        recount(c);
        campaignRepo.save(c);
        return toDetail(c);
    }

    @Override
    @Transactional
    public CampaignDetailDto cancel(UUID id) {
        WhatsappCampaign c = entity(id);
        if (c.getCampaignStatus() == WhatsappCampaignStatus.COMPLETED) {
            throw new BusinessException("A completed campaign cannot be cancelled.");
        }
        c.setCampaignStatus(WhatsappCampaignStatus.CANCELLED);
        return toDetail(campaignRepo.save(c));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        WhatsappCampaign c = entity(id);
        c.setDeleted(true);
        campaignRepo.save(c);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignAnalyticsDto analytics() {
        List<WhatsappCampaign> all = campaignRepo.findAll();
        long recipients = 0, sent = 0, delivered = 0, read = 0, failed = 0;
        for (WhatsappCampaign c : all) {
            recipients += c.getTotalRecipients();
            sent += c.getSentCount();
            delivered += c.getDeliveredCount();
            read += c.getReadCount();
            failed += c.getFailedCount();
        }
        double deliveryRate = sent == 0 ? 0 : round(delivered * 100.0 / sent);
        double failRate = recipients == 0 ? 0 : round(failed * 100.0 / recipients);
        return new CampaignAnalyticsDto(all.size(), recipients, sent, delivered, read, failed, deliveryRate, failRate);
    }

    @Override
    @Transactional(readOnly = true)
    public in.jlenterprises.ecommerce.dto.whatsapp.TestSendResult testSend(String phone, UUID templateId, String bodyText) {
        boolean demo = !whatsApp.isConfigured();
        WhatsappTemplate t = templateId == null ? null : templateRepo.findById(templateId).orElse(null);
        String body = t != null ? t.getBodyText() : (bodyText == null || bodyText.isBlank() ? "This is a test message from JL Enterprises." : bodyText);
        String rendered = render(body, "there");
        try {
            String id;
            if (demo) {
                id = "demo-" + UUID.randomUUID();
            } else if (t != null && t.getMetaTemplateName() != null && !t.getMetaTemplateName().isBlank()) {
                id = whatsApp.sendTemplate(phone, t.getMetaTemplateName(), t.getLanguage(), List.of("there"));
            } else {
                id = whatsApp.sendText(phone, rendered);
            }
            return new in.jlenterprises.ecommerce.dto.whatsapp.TestSendResult(true, demo, id, null);
        } catch (Exception e) {
            log.warn("WhatsApp test send failed for {}: {}", phone, e.getMessage());
            return new in.jlenterprises.ecommerce.dto.whatsapp.TestSendResult(false, demo, null, trim(e.getMessage(), 500));
        }
    }

    private boolean hasApprovedTemplate(WhatsappCampaign c) {
        return c.getTemplate() != null && c.getTemplate().getMetaTemplateName() != null
                && !c.getTemplate().getMetaTemplateName().isBlank();
    }

    // ── Legacy broadcast (mobile + old UI) ──
    @Override
    @Transactional
    @Auditable(action = "WHATSAPP_BROADCAST", entity = "whatsapp_campaign")
    public BroadcastResult quickBroadcast(String message, boolean onlyVerified) {
        List<User> recipients = userRepo.findByPhoneNotNull().stream()
                .filter(u -> !onlyVerified || u.isPhoneVerified())
                .toList();
        WhatsappCampaign c = new WhatsappCampaign();
        c.setName("Quick broadcast");
        c.setBodyText(message);
        c.setAudienceType(onlyVerified ? WhatsappAudienceType.VERIFIED_OPTED_IN : WhatsappAudienceType.EVERYONE_WITH_PHONE);
        c.setCampaignStatus(WhatsappCampaignStatus.DRAFT);
        c = campaignRepo.save(c);
        dispatch(c, recipients);
        return new BroadcastResult(c.getTotalRecipients(), c.getSentCount(), c.getFailedCount(), c.isDemoMode());
    }

    // ── Core send loop ──
    private void dispatch(WhatsappCampaign c, List<User> recipients) {
        boolean demo = !whatsApp.isConfigured();
        c.setDemoMode(demo);
        c.setCampaignStatus(WhatsappCampaignStatus.SENDING);
        c.setTotalRecipients(recipients.size());
        campaignRepo.save(c);

        for (User u : recipients) {
            WhatsappMessageLog msg = new WhatsappMessageLog();
            msg.setCampaign(c);
            msg.setUserId(u.getId());
            msg.setRecipientName(nameOf(u));
            msg.setPhone(u.getPhone());
            msg.setRenderedBody(render(c.getBodyText(), u.getFirstName()));
            sendOne(c, msg, demo);
            logRepo.save(msg);
        }
        recount(c);
        c.setSentAt(Instant.now());
        campaignRepo.save(c);
    }

    /** Send/simulate one message and set its status + provider id. Never throws. */
    private void sendOne(WhatsappCampaign c, WhatsappMessageLog msg, boolean demo) {
        msg.setAttempts(msg.getAttempts() + 1);
        try {
            String providerId;
            if (demo) {
                providerId = "demo-" + UUID.randomUUID();
            } else if (c.getTemplate() != null && c.getTemplate().getMetaTemplateName() != null
                    && !c.getTemplate().getMetaTemplateName().isBlank()) {
                providerId = whatsApp.sendTemplate(msg.getPhone(), c.getTemplate().getMetaTemplateName(),
                        c.getTemplate().getLanguage(), List.of(orThere(msg.getRecipientName())));
            } else {
                providerId = whatsApp.sendText(msg.getPhone(), msg.getRenderedBody());
            }
            msg.setProviderMessageId(providerId);
            msg.setMessageStatus(WhatsappMessageStatus.SENT);
            msg.setSentAt(Instant.now());
            msg.setError(null);
        } catch (Exception e) {
            msg.setMessageStatus(WhatsappMessageStatus.FAILED);
            msg.setError(trim(e.getMessage(), 500));
            log.warn("WhatsApp send failed for {}: {}", msg.getPhone(), e.getMessage());
        }
    }

    /** Tally sent/delivered/read/failed from the campaign's logs onto the campaign (no status change). */
    private void tallyCounts(WhatsappCampaign c) {
        List<WhatsappMessageLog> logs = logRepo.findByCampaign_IdOrderByCreatedAtAsc(c.getId());
        int sent = 0, delivered = 0, read = 0, failed = 0;
        for (WhatsappMessageLog m : logs) {
            switch (m.getMessageStatus()) {
                case SENT -> sent++;
                case DELIVERED -> { sent++; delivered++; }
                case READ -> { sent++; delivered++; read++; }
                case FAILED -> failed++;
                default -> { }
            }
        }
        c.setSentCount(sent);
        c.setDeliveredCount(delivered);
        c.setReadCount(read);
        c.setFailedCount(failed);
    }

    /** Recompute campaign counts from its logs and set the terminal status (used right after a send). */
    private void recount(WhatsappCampaign c) {
        tallyCounts(c);
        c.setCampaignStatus(c.getSentCount() == 0 && c.getFailedCount() > 0
                ? WhatsappCampaignStatus.FAILED : WhatsappCampaignStatus.COMPLETED);
    }

    // ── Delivery-status webhook roll-up + delivery log (Phase 2) ──
    @Override
    @Transactional
    public void recomputeCounts(UUID campaignId) {
        WhatsappCampaign c = campaignRepo.findById(campaignId).orElse(null);
        if (c == null) return;
        // Counts only — a webhook arriving after the send must not flip the terminal status.
        tallyCounts(c);
        campaignRepo.save(c);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryLogDto> deliveryLog(WhatsappMessageStatus status, UUID campaignId, String phone,
                                            Instant from, Instant to, org.springframework.data.domain.Pageable pageable) {
        String p = (phone == null || phone.isBlank()) ? null : phone.trim();
        return logRepo.search(status, campaignId, p, from, to, pageable).map(this::toDeliveryDto);
    }

    private DeliveryLogDto toDeliveryDto(WhatsappMessageLog m) {
        WhatsappCampaign c = m.getCampaign();
        return new DeliveryLogDto(m.getId(), c == null ? null : c.getId(), c == null ? null : c.getName(),
                m.getRecipientName(), m.getPhone(), m.getMessageStatus(), m.getProviderMessageId(),
                m.getError(), m.getAttempts(), m.getCreatedAt(), m.getSentAt(), m.getDeliveredAt(), m.getReadAt());
    }

    // ── Broadcast audience picker (Phase 3) ──
    @Override
    @Transactional(readOnly = true)
    public Page<AudienceCustomerDto> audienceCustomers(UUID categoryId, String city, Boolean optedIn,
                                                       Boolean phoneVerified, Boolean ordered, Boolean emi,
                                                       String search, Pageable pageable) {
        // Base: everyone contactable. Filters intersect id-sets (one query per active facet) —
        // audience sizes here are retailer-scale (thousands), so in-memory intersection is fine
        // and matches how the enum audiences already load full lists.
        List<User> users = userRepo.findByPhoneNotNull();

        Set<UUID> orderedIds = new HashSet<>(userRepo.findUserIdsWithOrders());
        if (Boolean.TRUE.equals(optedIn)) users = users.stream().filter(User::isWhatsappOptIn).toList();
        if (Boolean.TRUE.equals(phoneVerified)) users = users.stream().filter(User::isPhoneVerified).toList();
        if (ordered != null) {
            users = users.stream().filter(u -> orderedIds.contains(u.getId()) == ordered).toList();
        }
        if (categoryId != null) {
            Set<UUID> catIds = new HashSet<>(userRepo.findUserIdsWhoBoughtCategory(categoryId));
            users = users.stream().filter(u -> catIds.contains(u.getId())).toList();
        }
        if (city != null && !city.isBlank()) {
            Set<UUID> cityIds = new HashSet<>(userRepo.findUserIdsInCity(city.trim()));
            users = users.stream().filter(u -> cityIds.contains(u.getId())).toList();
        }
        if (Boolean.TRUE.equals(emi)) {
            Set<String> emiLast10 = userRepo.findEmiRequestPhones().stream()
                    .map(WhatsappCampaignServiceImpl::last10).filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            users = users.stream().filter(u -> emiLast10.contains(last10(u.getPhone()))).toList();
        }
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            users = users.stream().filter(u ->
                    nameOf(u).toLowerCase().contains(q)
                    || (u.getPhone() != null && u.getPhone().replaceAll("[^0-9]", "").contains(q.replaceAll("[^0-9]", "")) && !q.replaceAll("[^0-9]", "").isEmpty())
            ).toList();
        }

        List<User> sorted = users.stream().sorted(Comparator.comparing(this::nameOf, String.CASE_INSENSITIVE_ORDER)).toList();
        int start = (int) Math.min(pageable.getOffset(), sorted.size());
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        List<User> slice = sorted.subList(start, end);

        // Label just the visible page with each customer's saved city.
        java.util.Map<UUID, String> cityOf = new java.util.HashMap<>();
        if (!slice.isEmpty()) {
            for (Object[] row : userRepo.findCitiesForUsers(slice.stream().map(User::getId).toList())) {
                cityOf.put((UUID) row[0], (String) row[1]);
            }
        }
        List<AudienceCustomerDto> content = slice.stream().map(u -> new AudienceCustomerDto(
                u.getId(), nameOf(u), u.getPhone(), cityOf.get(u.getId()),
                u.isWhatsappOptIn(), u.isPhoneVerified(), orderedIds.contains(u.getId()))).toList();
        return new PageImpl<>(content, pageable, sorted.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> audienceCities() {
        return userRepo.findDistinctCities();
    }

    private static String last10(String phone) {
        if (phone == null) return null;
        String d = phone.replaceAll("[^0-9]", "");
        return d.length() < 10 ? null : d.substring(d.length() - 10);
    }

    // ── helpers ──
    private WhatsappCampaign entity(UUID id) {
        return campaignRepo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("WhatsappCampaign", id));
    }

    private String nameOf(User u) {
        String n = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return n.isEmpty() ? u.getEmail() : n;
    }

    private String render(String body, String firstName) {
        String n = orThere(firstName);
        return body.replace("{{name}}", n).replace("{name}", n).replace("{{firstName}}", n);
    }

    private static String orThere(String s) {
        return (s == null || s.isBlank()) ? "there" : s.trim().split("\\s+")[0];
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static double round(double d) {
        return Math.round(d * 10.0) / 10.0;
    }

    // ── DTO mapping ──
    private CampaignDto toSummary(WhatsappCampaign c) {
        return new CampaignDto(c.getId(), c.getName(), c.getAudienceType(), c.getCampaignStatus(), c.isDemoMode(),
                c.getTotalRecipients(), c.getSentCount(), c.getDeliveredCount(), c.getReadCount(), c.getFailedCount(),
                c.getScheduledAt(), c.getSentAt(), c.getCreatedAt());
    }

    private CampaignDetailDto toDetail(WhatsappCampaign c) {
        List<MessageLogDto> logs = logRepo.findByCampaign_IdOrderByCreatedAtAsc(c.getId()).stream()
                .map(m -> new MessageLogDto(m.getId(), m.getUserId(), m.getRecipientName(), m.getPhone(),
                        m.getRenderedBody(), m.getMessageStatus(), m.getProviderMessageId(), m.getError(),
                        m.getAttempts(), m.getSentAt()))
                .toList();
        int total = c.getTotalRecipients();
        double deliveryRate = total == 0 ? 0 : round(c.getDeliveredCount() * 100.0 / total);
        double readRate = total == 0 ? 0 : round(c.getReadCount() * 100.0 / total);
        double failRate = total == 0 ? 0 : round(c.getFailedCount() * 100.0 / total);
        return new CampaignDetailDto(c.getId(), c.getName(), c.getBodyText(),
                c.getTemplate() == null ? null : c.getTemplate().getName(),
                c.getAudienceType(), c.getCityFilter(), c.getCampaignStatus(), c.isDemoMode(),
                total, c.getSentCount(), c.getDeliveredCount(), c.getReadCount(), c.getFailedCount(),
                deliveryRate, readRate, failRate, c.getScheduledAt(), c.getSentAt(), c.getCreatedAt(), logs);
    }
}
