package in.jlenterprises.ecommerce.constant;

/** Order lifecycle. Allowed transitions are enforced in the order service. */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    PACKED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED,
    REFUNDED,
    FAILED_PAYMENT
}
