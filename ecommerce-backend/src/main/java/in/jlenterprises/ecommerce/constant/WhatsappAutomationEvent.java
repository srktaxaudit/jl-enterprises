package in.jlenterprises.ecommerce.constant;

/**
 * Store events that can trigger an automatic WhatsApp message to the customer.
 * Each event has at most one {@code WhatsappAutomationRule} mapping it to a template.
 */
public enum WhatsappAutomationEvent {
    ORDER_PLACED,
    PAYMENT_RECEIVED,
    ORDER_SHIPPED,
    ORDER_OUT_FOR_DELIVERY,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    /** An unpaid online order auto-expired — a "complete your purchase" nudge. */
    ABANDONED_CHECKOUT
}
