package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.inventory.InventoryDto;
import in.jlenterprises.ecommerce.request.inventory.InventoryUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface InventoryService {

    InventoryDto getByProduct(UUID productId);

    InventoryDto update(UUID productId, InventoryUpdateRequest request);

    List<InventoryDto> lowStock();
}
