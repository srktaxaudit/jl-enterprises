package in.jlenterprises.ecommerce.constant;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED,
    /** The order was cancelled before payment was ever collected. Terminal: keeps
        never-collected payments out of the billing "pending" bucket forever. */
    CANCELLED
}
