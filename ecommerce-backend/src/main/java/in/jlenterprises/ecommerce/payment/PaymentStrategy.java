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

    /**
     * Refund the captured amount AT THE GATEWAY. Returns the provider's refund reference,
     * or {@code null} when this method has no gateway to call (COD refunds are handed back
     * in cash/UPI by staff). Throws {@code BusinessException} when the gateway call fails —
     * callers must NOT mark anything refunded in that case.
     */
    default String refund(Payment payment) {
        return null;
    }

    /**
     * Reconciliation: ask the provider whether a captured payment exists for this
     * provider order (e.g. the customer paid but the browser died before our confirm
     * callback). Returns the provider payment id if captured, else {@code null}.
     */
    default String capturedPaymentId(Payment payment) {
        return null;
    }
}
