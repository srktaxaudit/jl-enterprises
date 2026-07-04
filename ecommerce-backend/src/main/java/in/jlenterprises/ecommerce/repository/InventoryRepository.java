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

    // All queries join i.product so Hibernate applies Product's @SQLRestriction
    // (deleted = false) — inventory rows for soft-deleted products are excluded,
    // which is what previously caused the low-stock endpoint to 500.

    @Query("select i from Inventory i join i.product p where (i.quantity - i.reserved) <= i.reorderLevel")
    List<Inventory> findLowStock();

    @Query("select count(i) from Inventory i join i.product p where (i.quantity - i.reserved) <= i.reorderLevel")
    long countLowStock();

    @Query("select count(i) from Inventory i join i.product p")
    long countActive();

    @Query("select count(i) from Inventory i join i.product p where (i.quantity - i.reserved) > 0")
    long countInStock();

    @Query("select count(i) from Inventory i join i.product p where (i.quantity - i.reserved) <= 0")
    long countOutOfStock();
}
