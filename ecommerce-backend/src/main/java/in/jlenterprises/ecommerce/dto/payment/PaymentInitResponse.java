package in.jlenterprises.ecommerce.dto.payment;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

/** What the client needs to complete payment after initiation. */
public record PaymentInitResponse(
        UUID paymentId,
        PaymentMethod method,
        PaymentStatus status,
        String provider,
        String providerReference,
        String clientData,
        BigDecimal amount,
        String currency
) {}
