package in.jlenterprises.ecommerce.constant;

/** Delivery state of a single WhatsApp message. DELIVERED/READ arrive via webhooks (Phase 2). */
public enum WhatsappMessageStatus {
    QUEUED,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
