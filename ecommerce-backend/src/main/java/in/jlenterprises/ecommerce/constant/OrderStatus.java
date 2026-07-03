package in.jlenterprises.ecommerce.constant;

/** Order lifecycle. Allowed transitions are enforced in the order service. */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PACKED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
