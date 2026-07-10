package in.jlenterprises.ecommerce.service;

import com.fasterxml.jackson.databind.JsonNode;

/** Processes inbound Meta WhatsApp webhook payloads (delivery statuses now; inbound messages in Phase 5). */
public interface WhatsappWebhookService {

    /** Apply a webhook payload. Returns the number of message-status updates applied. Never throws. */
    int ingest(JsonNode payload);
}
