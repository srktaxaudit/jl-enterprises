package in.jlenterprises.ecommerce.dto.order;

import in.jlenterprises.ecommerce.constant.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID id,
        String orderNumber,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxTotal,
        BigDecimal shippingTotal,
        BigDecimal grandTotal,
        String currency,
        String couponCode,
        AddressSnapshotDto shippingAddress,
        AddressSnapshotDto billingAddress,
        String notes,
        Instant placedAt,
        List<OrderItemDto> items,
        OrderPaymentDto payment
) {}
