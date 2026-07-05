package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.dto.inventory.InventoryDto;
import in.jlenterprises.ecommerce.request.inventory.InventoryUpdateRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER','INVENTORY_MANAGER')")
@Tag(name = "Admin — Inventory", description = "Stock levels and reorder thresholds (staff)")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List products at or below their reorder level")
    public ApiResponse<List<InventoryDto>> lowStock() {
        return ApiResponse.success(inventoryService.lowStock());
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get a product's stock record")
    public ApiResponse<InventoryDto> get(@PathVariable UUID productId) {
        return ApiResponse.success(inventoryService.getByProduct(productId));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Set a product's stock quantity, reorder level and location")
    public ApiResponse<InventoryDto> update(@PathVariable UUID productId,
                                            @Valid @RequestBody InventoryUpdateRequest request) {
        return ApiResponse.success("Inventory updated", inventoryService.update(productId, request));
    }
}
