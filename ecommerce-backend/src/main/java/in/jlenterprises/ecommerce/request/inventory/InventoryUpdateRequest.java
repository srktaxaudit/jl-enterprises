package in.jlenterprises.ecommerce.request.inventory;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record InventoryUpdateRequest(
        @PositiveOrZero int quantity,
        @PositiveOrZero int reorderLevel,
        @Size(max = 120) String warehouseLocation
) {}
