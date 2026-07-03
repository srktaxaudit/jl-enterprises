package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    @Query("select i from Inventory i where (i.quantity - i.reserved) <= i.reorderLevel")
    List<Inventory> findLowStock();

    @Query("select count(i) from Inventory i where (i.quantity - i.reserved) <= i.reorderLevel")
    long countLowStock();
}
