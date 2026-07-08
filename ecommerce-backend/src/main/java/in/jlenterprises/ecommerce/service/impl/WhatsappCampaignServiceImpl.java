package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.WhatsappAudienceType;
import in.jlenterprises.ecommerce.constant.WhatsappCampaignStatus;
import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;
import in.jlenterprises.ecommerce.dto.admin.BroadcastResult;
import in.jlenterprises.ecommerce.dto.whatsapp.AudiencePreviewDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignAnalyticsDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignDetailDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WhatsappCampaignServiceImpl implements WhatsappCampaignService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappCampaignServiceImpl.class);

    private final WhatsappCampaignRepository campaignRepo;
    private final WhatsappMessageLogRepository logRepo;
    private final WhatsappTemplateRepository templateRepo;
    private final UserRepository userRepo;
    private final WhatsAppService whatsApp;

    public WhatsappCampaignServiceImpl(WhatsappCampaignRepository campaignRepo,
                                       WhatsappMessageLogRepository logRepo,
                                       WhatsappTemplateRepository templateRepo,
                                       UserRepository userRepo,
                                       WhatsAppService whatsApp) {
        this.campaignRepo = campaignRepo;
        this.logRepo = logRepo;
        this.templateRepo = templateRepo;
        this.userRepo = userRepo;
        this.whatsApp = whatsApp;
    }

    // ── Audience ──
    private List<User> resolveAudience(WhatsappAudienceType type) {
        return switch (type) {
            case ALL_OPTED_IN -> userRepo.findByWhatsappOptInTrueAndPhoneNotNull();
            case VERIFIED_OPTED_IN -> userRepo.findByWhatsappOptInTrueAndPhoneVerifiedTrueAndPhoneNotNull();
            case HAS_ORDERED -> userRepo.findOptedInWithOrders();
            case EVERYONE_WITH_PHONE -> userRepo.findByPhoneNotNull();
        };
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
        List<User> recipients = resolveAudience(c.getAudienceType());
        if (recipients.isEmpty()) {
            throw new BusinessException("No customers match this audience yet.");
        }
        // Live marketing to opted-in customers must use an approved template — Meta
        // rejects free text outside the 24h reply window. Demo mode simulates, so allow it.
        if (whatsApp.isConfigured() && !hasApprovedTemplate(c)) {
            throw new BusinessException("Live sending needs an approved template. Create a template with its "
                    + "Meta template name, then compose the campaign from it. (Free text only works inside the 24-hour reply window.)");
        }
        dispatch(c, recipients);
        return toDetail(c);
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

    /** Recompute campaign counts from its logs and set the terminal status. */
    private void recount(WhatsappCampaign c) {
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
        c.setCampaignStatus(sent == 0 && failed > 0 ? WhatsappCampaignStatus.FAILED : WhatsappCampaignStatus.COMPLETED);
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
