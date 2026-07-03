package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.dto.inventory.InventoryDto;
import in.jlenterprises.ecommerce.entity.Inventory;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.InventoryRepository;
import in.jlenterprises.ecommerce.request.inventory.InventoryUpdateRequest;
import in.jlenterprises.ecommerce.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryDto getByProduct(UUID productId) {
        return toDto(getEntity(productId));
    }

    @Override
    @Transactional
    public InventoryDto update(UUID productId, InventoryUpdateRequest request) {
        Inventory inv = getEntity(productId);
        inv.setQuantity(request.quantity());
        inv.setReorderLevel(request.reorderLevel());
        inv.setWarehouseLocation(request.warehouseLocation());
        return toDto(inventoryRepository.save(inv));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryDto> lowStock() {
        return inventoryRepository.findLowStock().stream().map(this::toDto).toList();
    }

    private Inventory getEntity(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inventory for product", productId));
    }

    private InventoryDto toDto(Inventory inv) {
        return new InventoryDto(
                inv.getProduct().getId(), inv.getProduct().getName(),
                inv.getQuantity(), inv.getReserved(), inv.getAvailable(),
                inv.getReorderLevel(), inv.getWarehouseLocation());
    }
}
