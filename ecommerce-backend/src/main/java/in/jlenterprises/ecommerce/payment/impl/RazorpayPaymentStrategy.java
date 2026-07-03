package in.jlenterprises.ecommerce.payment.impl;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.payment.PaymentConfirmation;
import in.jlenterprises.ecommerce.payment.PaymentInitResult;
import in.jlenterprises.ecommerce.payment.PaymentStrategy;
import org.springframework.stereotype.Component;

/**
 * Razorpay integration point. TODO: replace the stub body with the Razorpay
 * SDK — create an order in {@link #initiate} and verify the HMAC signature in
 * {@link #verify}. Kept as a working placeholder so the flow is testable.
 */
@Component
public class RazorpayPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.RAZORPAY;
    }

    @Override
    public PaymentInitResult initiate(Payment payment) {
        String ref = "rzp_order_" + payment.getOrder().getOrderNumber();
        return new PaymentInitResult("razorpay", ref,
                "Razorpay keys not configured — set RAZORPAY_KEY_ID/SECRET", PaymentStatus.PENDING);
    }

    @Override
    public boolean verify(Payment payment, PaymentConfirmation confirmation) {
        // TODO: verify HMAC(order_id|payment_id) against RAZORPAY_KEY_SECRET.
        return confirmation != null && confirmation.providerReference() != null
                && !confirmation.providerReference().isBlank();
    }
}
