package in.jlenterprises.ecommerce.dto.order;

import in.jlenterprises.ecommerce.constant.OrderStatus;

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
        Instant placedAt
) {}
