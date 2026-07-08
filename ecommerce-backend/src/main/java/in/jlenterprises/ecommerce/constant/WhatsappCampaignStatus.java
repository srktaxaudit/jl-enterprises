package in.jlenterprises.ecommerce.constant;

/** Lifecycle of a WhatsApp marketing campaign. SCHEDULED is reserved for Phase 2. */
public enum WhatsappCampaignStatus {
    DRAFT,
    SCHEDULED,
    SENDING,
    COMPLETED,
    FAILED,
    CANCELLED
}
