package in.jlenterprises.ecommerce.dto.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDto(
        UUID id,
        UUID productId,
        String productName,
        String slug,
        String primaryImageUrl,
        UUID variantId,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {}
