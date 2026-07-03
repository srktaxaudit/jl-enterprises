package in.jlenterprises.ecommerce.payment;

import in.jlenterprises.ecommerce.constant.PaymentStatus;

/**
 * Result of initiating a payment.
 *
 * @param provider           provider name (e.g. "razorpay")
 * @param providerReference  provider-side id (order/intent id) to persist
 * @param clientData         data the browser SDK needs (client secret, order id, or a note)
 * @param status             the payment status immediately after initiation
 */
public record PaymentInitResult(
        String provider,
        String providerReference,
        String clientData,
        PaymentStatus status
) {}
