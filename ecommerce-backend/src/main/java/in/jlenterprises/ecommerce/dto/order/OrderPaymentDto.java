package in.jlenterprises.ecommerce.dto.order;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;

import java.math.BigDecimal;

public record OrderPaymentDto(
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String providerPaymentId
) {}
