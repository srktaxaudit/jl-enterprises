package in.jlenterprises.ecommerce.dto.inventory;

import java.util.UUID;

public record InventoryDto(
        UUID productId,
        String productName,
        int quantity,
        int reserved,
        int available,
        int reorderLevel,
        String warehouseLocation
) {}
