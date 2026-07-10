package in.jlenterprises.ecommerce.constant;

/** Who a campaign targets. Opted-in audiences are the compliant default for marketing. */
public enum WhatsappAudienceType {
    ALL_OPTED_IN,
    VERIFIED_OPTED_IN,
    HAS_ORDERED,
    EVERYONE_WITH_PHONE,
    /** Hand-picked recipients chosen from the audience picker (Phase 3). */
    MANUAL
}
