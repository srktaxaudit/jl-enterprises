package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.dto.inventory.InventoryDto;
import in.jlenterprises.ecommerce.entity.Inventory;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.InventoryRepository;
import in.jlenterprises.ecommerce.request.inventory.InventoryUpdateRequest;
import in.jlenterprises.ecommerce.service.InventoryService;
import in.jlenterprises.ecommerce.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final InventoryRepository inventoryRepository;
    private final NotificationService notificationService;

    public InventoryServiceImpl(InventoryRepository inventoryRepository, NotificationService notificationService) {
        this.inventoryRepository = inventoryRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryDto getByProduct(UUID productId) {
        return toDto(getEntity(productId));
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE_INVENTORY", entity = "inventory")
    public InventoryDto update(UUID productId, InventoryUpdateRequest request) {
        // Row lock (SELECT ... FOR UPDATE), same as the checkout deduct path: writing an
        // absolute quantity through a plain read could silently overwrite a concurrent
        // checkout's decrement in the window between our read and our save.
        Inventory inv = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseGet(() -> getEntity(productId));
        inv.setQuantity(request.quantity());
        inv.setReorderLevel(request.reorderLevel());
        inv.setWarehouseLocation(request.warehouseLocation());
        Inventory saved = inventoryRepository.save(inv);

        // Alert admins when stock drops to/below the reorder level (or runs out).
        String status = stockStatus(saved);
        if (!"IN_STOCK".equals(status)) {
            String product = saved.getProduct().getName();
            notificationService.notifyAdmins(NotificationType.SYSTEM,
                    "OUT_OF_STOCK".equals(status) ? "Out of stock: " + product : "Low stock: " + product,
                    product + " has " + saved.getAvailable() + " available (reorder at " + saved.getReorderLevel() + ").",
                    "/admin-inventory.html");
        }
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryDto> lowStock() {
        try {
            return inventoryRepository.findLowStock().stream().map(this::toDto).toList();
        } catch (Exception e) {
            // Surface the real cause in the logs rather than a generic 500.
            log.error("Failed to load low-stock inventory: {}", e.toString(), e);
            throw e;
        }
    }

    private Inventory getEntity(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inventory for product", productId));
    }

    private InventoryDto toDto(Inventory inv) {
        return new InventoryDto(
                inv.getProduct().getId(), inv.getProduct().getName(),
                inv.getQuantity(), inv.getReserved(), inv.getAvailable(),
                inv.getReorderLevel(), inv.getWarehouseLocation(),
                stockStatus(inv));
    }

    /** IN_STOCK / LOW_STOCK / OUT_OF_STOCK from available vs reorder level. */
    static String stockStatus(Inventory inv) {
        int available = inv.getAvailable();
        if (available <= 0) return "OUT_OF_STOCK";
        if (available <= inv.getReorderLevel()) return "LOW_STOCK";
        return "IN_STOCK";
    }
}
