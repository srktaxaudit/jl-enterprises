package in.jlenterprises.ecommerce.payment.impl;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.payment.PaymentConfirmation;
import in.jlenterprises.ecommerce.payment.PaymentInitResult;
import in.jlenterprises.ecommerce.payment.PaymentStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Stripe integration point — NOT yet implemented. Until a real PaymentIntent
 * create/confirm (and webhook) is wired up, this strategy must refuse: it never
 * initiates, and {@link #verify} always returns false so an order can never be
 * marked paid without a real gateway confirmation.
 */
@Component
public class StripePaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.STRIPE;
    }

    @Override
    public PaymentInitResult initiate(Payment payment) {
        throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                "Stripe payments are not available. Please choose Cash on Delivery.");
    }

    @Override
    public boolean verify(Payment payment, PaymentConfirmation confirmation) {
        // No real gateway verification exists — never confirm a Stripe payment.
        return false;
    }
}
