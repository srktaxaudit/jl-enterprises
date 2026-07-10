package in.jlenterprises.ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.entity.WhatsappChatMessage;
import in.jlenterprises.ecommerce.entity.WhatsappConversation;
import in.jlenterprises.ecommerce.entity.WhatsappMessageLog;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.repository.WhatsappChatMessageRepository;
import in.jlenterprises.ecommerce.repository.WhatsappConversationRepository;
import in.jlenterprises.ecommerce.repository.WhatsappMessageLogRepository;
import in.jlenterprises.ecommerce.service.WhatsappCampaignService;
import in.jlenterprises.ecommerce.service.WhatsappWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WhatsappWebhookServiceImpl implements WhatsappWebhookService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookServiceImpl.class);

    private final WhatsappMessageLogRepository logRepo;
    private final WhatsappCampaignService campaigns;
    private final WhatsappConversationRepository conversationRepo;
    private final WhatsappChatMessageRepository chatRepo;
    private final UserRepository userRepo;

    public WhatsappWebhookServiceImpl(WhatsappMessageLogRepository logRepo, WhatsappCampaignService campaigns,
                                      WhatsappConversationRepository conversationRepo,
                                      WhatsappChatMessageRepository chatRepo, UserRepository userRepo) {
        this.logRepo = logRepo;
        this.campaigns = campaigns;
        this.conversationRepo = conversationRepo;
        this.chatRepo = chatRepo;
        this.userRepo = userRepo;
    }

    @Override
    @Transactional
    public int ingest(JsonNode payload) {
        if (payload == null) return 0;
        Set<UUID> affectedCampaigns = new HashSet<>();
        int updated = 0;
        JsonNode entries = payload.path("entry");
        if (entries.isArray()) {
            for (JsonNode entry : entries) {
                JsonNode changes = entry.path("changes");
                if (!changes.isArray()) continue;
                for (JsonNode change : changes) {
                    JsonNode value = change.path("value");
                    JsonNode statuses = value.path("statuses");
                    if (statuses.isArray()) {
                        for (JsonNode s : statuses) {
                            if (applyStatus(s, affectedCampaigns)) updated++;
                        }
                    }
                    JsonNode messages = value.path("messages");
                    if (messages.isArray()) {
                        String contactName = extractContactName(value);
                        for (JsonNode m : messages) {
                            if (applyInbound(m, contactName)) updated++;
                        }
                    }
                }
            }
        }
        for (UUID campaignId : affectedCampaigns) {
            try {
                campaigns.recomputeCounts(campaignId);
            } catch (Exception e) {
                log.warn("Could not recompute counts for campaign {}: {}", campaignId, e.getMessage());
            }
        }
        return updated;
    }

    /** Update one message log from a status entry. Returns true if a log was found and touched. */
    private boolean applyStatus(JsonNode s, Set<UUID> affected) {
        String providerId = text(s, "id");
        WhatsappMessageStatus next = map(text(s, "status"));
        if (providerId == null || next == null) return false;

        WhatsappMessageLog m = logRepo.findByProviderMessageId(providerId).orElse(null);
        if (m == null) return applyChatStatus(providerId, next, s);

        Instant ts = parseTimestamp(s.path("timestamp"));
        if (shouldAdvance(m.getMessageStatus(), next)) {
            m.setMessageStatus(next);
        }
        switch (next) {
            case SENT -> { if (m.getSentAt() == null) m.setSentAt(ts); }
            case DELIVERED -> {
                if (m.getSentAt() == null) m.setSentAt(ts);
                if (m.getDeliveredAt() == null) m.setDeliveredAt(ts);
            }
            case READ -> {
                if (m.getSentAt() == null) m.setSentAt(ts);
                if (m.getDeliveredAt() == null) m.setDeliveredAt(ts);
                if (m.getReadAt() == null) m.setReadAt(ts);
            }
            case FAILED -> m.setError(extractError(s));
            default -> { }
        }
        logRepo.save(m);
        if (m.getCampaign() != null) affected.add(m.getCampaign().getId());
        return true;
    }

    /** Status update for an Inbox chat reply (Phase 5). Returns true when a chat message matched. */
    private boolean applyChatStatus(String providerId, WhatsappMessageStatus next, JsonNode s) {
        WhatsappChatMessage m = chatRepo.findByProviderMessageId(providerId).orElse(null);
        if (m == null) return false;
        if (shouldAdvance(m.getMessageStatus(), next)) {
            m.setMessageStatus(next);
            if (next == WhatsappMessageStatus.FAILED) m.setError(extractError(s));
            chatRepo.save(m);
        }
        return true;
    }

    /** Store one inbound customer message and roll it up onto its conversation (Phase 5 Inbox). */
    private boolean applyInbound(JsonNode m, String contactName) {
        String providerId = text(m, "id");
        String from = text(m, "from");
        if (from == null || from.isBlank()) return false;
        if (providerId != null && chatRepo.existsByProviderMessageId(providerId)) return false; // webhook retry

        String type = m.path("type").asText("text");
        String body = "text".equals(type)
                ? m.path("text").path("body").asText("")
                : ("button".equals(type)
                        ? m.path("button").path("text").asText("[button reply]")
                        : "[" + type + " message]");
        if (body.isBlank()) body = "[" + type + " message]";
        if (body.length() > 2000) body = body.substring(0, 2000);
        Instant at = parseTimestamp(m.path("timestamp"));

        WhatsappConversation conv = conversationRepo.findByPhone(from).orElseGet(() -> {
            WhatsappConversation c = new WhatsappConversation();
            c.setPhone(from);
            c.setUserId(matchUserId(from));
            return c;
        });
        if (contactName != null && !contactName.isBlank()) conv.setContactName(contactName);
        conv.setLastMessageAt(at);
        conv.setLastInboundAt(at);
        conv.setLastPreview(body.length() > 200 ? body.substring(0, 200) : body);
        conv.setUnreadCount(conv.getUnreadCount() + 1);
        conv = conversationRepo.save(conv);

        WhatsappChatMessage msg = new WhatsappChatMessage();
        msg.setConversation(conv);
        msg.setDirection("IN");
        msg.setMessageType(type.length() > 20 ? type.substring(0, 20) : type);
        msg.setBody(body);
        msg.setProviderMessageId(providerId);
        msg.setMessageStatus(WhatsappMessageStatus.DELIVERED);
        msg.setEventAt(at);
        chatRepo.save(msg);
        return true;
    }

    /** Best-effort: map the sender's number to a store customer by its last 10 digits. */
    private UUID matchUserId(String phone) {
        try {
            String d = phone.replaceAll("[^0-9]", "");
            if (d.length() < 10) return null;
            List<User> users = userRepo.findByPhoneLast10(d.substring(d.length() - 10));
            return users.isEmpty() ? null : users.get(0).getId();
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractContactName(JsonNode value) {
        JsonNode contacts = value.path("contacts");
        if (contacts.isArray() && !contacts.isEmpty()) {
            return contacts.get(0).path("profile").path("name").asText(null);
        }
        return null;
    }

    /** Forward-only transitions (queued<sent<delivered<read); FAILED only applies before delivery. */
    private static boolean shouldAdvance(WhatsappMessageStatus current, WhatsappMessageStatus next) {
        if (next == WhatsappMessageStatus.FAILED) {
            return current == WhatsappMessageStatus.QUEUED || current == WhatsappMessageStatus.SENT;
        }
        return rank(next) > rank(current);
    }

    private static int rank(WhatsappMessageStatus s) {
        return switch (s) {
            case QUEUED -> 0;
            case SENT -> 1;
            case DELIVERED -> 2;
            case READ -> 3;
            case FAILED -> 1; // treated like "sent then failed" for ordering
        };
    }

    private static WhatsappMessageStatus map(String metaStatus) {
        if (metaStatus == null) return null;
        return switch (metaStatus.toLowerCase()) {
            case "sent" -> WhatsappMessageStatus.SENT;
            case "delivered" -> WhatsappMessageStatus.DELIVERED;
            case "read" -> WhatsappMessageStatus.READ;
            case "failed" -> WhatsappMessageStatus.FAILED;
            default -> null; // "deleted", "warning", etc. ignored
        };
    }

    private static Instant parseTimestamp(JsonNode ts) {
        try {
            if (ts != null && !ts.isMissingNode() && !ts.isNull()) {
                return Instant.ofEpochSecond(Long.parseLong(ts.asText()));
            }
        } catch (Exception ignore) {
            // fall through
        }
        return Instant.now();
    }

    private static String extractError(JsonNode s) {
        JsonNode errors = s.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            JsonNode e = errors.get(0);
            String title = e.path("title").asText(null);
            String detail = e.path("error_data").path("details").asText(null);
            String msg = (title == null ? "" : title) + (detail == null || detail.isBlank() ? "" : " — " + detail);
            if (!msg.isBlank()) return msg.length() > 500 ? msg.substring(0, 500) : msg;
        }
        return "Delivery failed";
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
