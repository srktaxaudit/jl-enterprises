package in.jlenterprises.ecommerce.constant;

/** Lifecycle of a customer exchange (trade-in) request. */
public enum ExchangeStatus {
    PENDING,
    UNDER_REVIEW,
    INSPECTION_SCHEDULED,
    OFFER_SENT,
    APPROVED,
    REJECTED,
    COMPLETED,
    CANCELLED
}
