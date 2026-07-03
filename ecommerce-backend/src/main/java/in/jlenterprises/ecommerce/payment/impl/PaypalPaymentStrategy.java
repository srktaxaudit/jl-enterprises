package in.jlenterprises.ecommerce.payment.impl;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.payment.PaymentConfirmation;
import in.jlenterprises.ecommerce.payment.PaymentInitResult;
import in.jlenterprises.ecommerce.payment.PaymentStrategy;
import org.springframework.stereotype.Component;

/**
 * PayPal integration point. TODO: create an order via the Orders v2 API in
 * {@link #initiate} and capture it in {@link #verify}. Placeholder for now.
 */
@Component
public class PaypalPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.PAYPAL;
    }

    @Override
    public PaymentInitResult initiate(Payment payment) {
        String ref = "pp_" + payment.getOrder().getOrderNumber();
        return new PaymentInitResult("paypal", ref,
                "PayPal keys not configured — set PAYPAL_CLIENT_ID/SECRET", PaymentStatus.PENDING);
    }

    @Override
    public boolean verify(Payment payment, PaymentConfirmation confirmation) {
        // TODO: capture the PayPal order and check status == "COMPLETED".
        return confirmation != null && confirmation.providerReference() != null
                && !confirmation.providerReference().isBlank();
    }
}
