package in.jlenterprises.ecommerce.payment;

/** Data returned by the client/provider after the customer completes payment. */
public record PaymentConfirmation(
        String providerReference,
        String signature,
        String payload
) {}
