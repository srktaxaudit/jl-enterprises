package in.jlenterprises.ecommerce.payment.impl;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.payment.PaymentConfirmation;
import in.jlenterprises.ecommerce.payment.PaymentInitResult;
import in.jlenterprises.ecommerce.payment.PaymentStrategy;
import org.springframework.stereotype.Component;

/** Cash on delivery — no gateway; collected at delivery, so it stays PENDING until fulfilled. */
@Component
public class CodPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.COD;
    }

    @Override
    public PaymentInitResult initiate(Payment payment) {
        return new PaymentInitResult("cod", payment.getOrder().getOrderNumber(),
                "Pay cash on delivery", PaymentStatus.PENDING);
    }

    @Override
    public boolean verify(Payment payment, PaymentConfirmation confirmation) {
        // COD is settled at delivery; treat confirmation as acknowledged.
        return true;
    }
}
