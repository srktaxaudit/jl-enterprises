package in.jlenterprises.ecommerce.payment.impl;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.payment.PaymentConfirmation;
import in.jlenterprises.ecommerce.payment.PaymentInitResult;
import in.jlenterprises.ecommerce.payment.PaymentStrategy;
import org.springframework.stereotype.Component;

/**
 * Stripe integration point. TODO: create a PaymentIntent in {@link #initiate}
 * (return its client_secret) and confirm it / handle the webhook in
 * {@link #verify}. Placeholder implementation for now.
 */
@Component
public class StripePaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.STRIPE;
    }

    @Override
    public PaymentInitResult initiate(Payment payment) {
        String ref = "pi_" + payment.getOrder().getOrderNumber();
        return new PaymentInitResult("stripe", ref,
                "Stripe keys not configured — set STRIPE_SECRET_KEY", PaymentStatus.PENDING);
    }

    @Override
    public boolean verify(Payment payment, PaymentConfirmation confirmation) {
        // TODO: retrieve the PaymentIntent and check status == "succeeded".
        return confirmation != null && confirmation.providerReference() != null
                && !confirmation.providerReference().isBlank();
    }
}
