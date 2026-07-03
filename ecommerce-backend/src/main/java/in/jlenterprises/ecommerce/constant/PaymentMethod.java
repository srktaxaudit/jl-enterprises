package in.jlenterprises.ecommerce.constant;

/** Payment providers. Each maps to a {@code PaymentStrategy} implementation. */
public enum PaymentMethod {
    STRIPE,
    RAZORPAY,
    PAYPAL,
    COD
}
