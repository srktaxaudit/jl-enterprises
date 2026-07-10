package in.jlenterprises.ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;
import in.jlenterprises.ecommerce.entity.WhatsappMessageLog;
import in.jlenterprises.ecommerce.repository.WhatsappMessageLogRepository;
import in.jlenterprises.ecommerce.service.WhatsappCampaignService;
import in.jlenterprises.ecommerce.service.WhatsappWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class WhatsappWebhookServiceImpl implements WhatsappWebhookService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookServiceImpl.class);

    private final WhatsappMessageLogRepository logRepo;
    private final WhatsappCampaignService campaigns;

    public WhatsappWebhookServiceImpl(WhatsappMessageLogRepository logRepo, WhatsappCampaignService campaigns) {
        this.logRepo = logRepo;
        this.campaigns = campaigns;
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
                    JsonNode statuses = change.path("value").path("statuses");
                    if (statuses.isArray()) {
                        for (JsonNode s : statuses) {
                            if (applyStatus(s, affectedCampaigns)) updated++;
                        }
                    }
                    // change.path("value").path("messages") = inbound messages -> Phase 5 (Inbox). Ignored here.
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
        if (m == null) return false;

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
