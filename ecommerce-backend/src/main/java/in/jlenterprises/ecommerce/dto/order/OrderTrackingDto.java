package in.jlenterprises.ecommerce.dto.order;

import in.jlenterprises.ecommerce.constant.OrderStatus;

import java.time.Instant;

/** Public order-tracking projection (no personal data beyond status + placed date). */
public record OrderTrackingDto(
        String orderNumber,
        OrderStatus status,
        Instant placedAt
) {}
