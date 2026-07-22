package in.jlenterprises.ecommerce.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID id,
        UUID productId,
        UUID variantId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        /* GST snapshot frozen at order time (null on legacy rows → store default rate). */
        BigDecimal gstRate,
        String hsnCode
) {}
