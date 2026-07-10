package in.jlenterprises.ecommerce.dto.stock;

import java.time.Instant;
import java.util.UUID;

public record StockAlertDto(
        UUID id,
        String customerName,
        String phone,
        UUID productId,
        String productName,
        String alertStatus,
        Instant createdAt
) {}
