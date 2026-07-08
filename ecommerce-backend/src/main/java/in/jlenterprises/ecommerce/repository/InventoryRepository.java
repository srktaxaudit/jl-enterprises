package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    /** Batch lookup for product list pages — one query per page, not per product. */
    List<Inventory> findByProductIdIn(java.util.Collection<UUID> productIds);

    /**
     * Fetch the inventory row with a pessimistic write lock (SELECT ... FOR UPDATE),
     * so concurrent checkouts serialize on it and cannot oversell. Use only inside
     * the order-placement transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.product.id = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") UUID productId);

    // Explicitly filter p.deleted = false (don't rely on @SQLRestriction being
    // applied to the join) so inventory rows for soft-deleted products are excluded.
    // findLowStock uses JOIN FETCH so the product is eagerly loaded — the mapper then
    // never triggers a lazy load of a (possibly soft-deleted) product, which was the
    // original cause of the low-stock 500.

    @Query("select i from Inventory i join fetch i.product p "
            + "where p.deleted = false and (i.quantity - i.reserved) <= i.reorderLevel")
    List<Inventory> findLowStock();

    @Query("select count(i) from Inventory i join i.product p "
            + "where p.deleted = false and (i.quantity - i.reserved) <= i.reorderLevel")
    long countLowStock();

    @Query("select count(i) from Inventory i join i.product p where p.deleted = false")
    long countActive();

    @Query("select count(i) from Inventory i join i.product p "
            + "where p.deleted = false and (i.quantity - i.reserved) > 0")
    long countInStock();

    @Query("select count(i) from Inventory i join i.product p "
            + "where p.deleted = false and (i.quantity - i.reserved) <= 0")
    long countOutOfStock();
}
