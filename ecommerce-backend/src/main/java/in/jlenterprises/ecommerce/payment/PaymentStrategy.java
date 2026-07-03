package in.jlenterprises.ecommerce.payment;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.entity.Payment;

/**
 * Strategy for a payment provider. Each supported {@link PaymentMethod} has one
 * implementation; the {@link PaymentStrategyFactory} selects it at runtime. This
 * keeps provider-specific logic isolated and makes adding a gateway a matter of
 * dropping in a new bean — no changes to the order or payment services.
 */
public interface PaymentStrategy {

    PaymentMethod method();

    /** Begin a payment: create the provider-side intent/order and return what the client needs. */
    PaymentInitResult initiate(Payment payment);

    /** Verify a completed payment (signature/charge check). @return true if genuinely paid. */
    boolean verify(Payment payment, PaymentConfirmation confirmation);
}
