package in.jlenterprises.ecommerce.dto.order;

import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryDto(
        UUID id,
        String orderNumber,
        OrderStatus status,
        BigDecimal grandTotal,
        String currency,
        int itemCount,
        Instant placedAt,
        /* Lets My Orders offer "Complete payment" for a pending online payment. */
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus
) {}
