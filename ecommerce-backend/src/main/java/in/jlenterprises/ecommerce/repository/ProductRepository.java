package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Product data access. Extends {@link JpaSpecificationExecutor} so the service
 * layer can compose dynamic search/filter/sort criteria (see ProductSpecifications).
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku);

    /** Products imported from Vyapar — every migrated SKU is VYP-prefixed (for rollback). */
    java.util.List<Product> findBySkuStartingWith(String prefix);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    Page<Product> findByFeaturedTrue(Pageable pageable);

    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    /** Products with no category or still in "General" — for the auto-categorizer.
        Category is join-fetched to avoid an N+1 lazy load per product. */
    @org.springframework.data.jpa.repository.Query(
            "select p from Product p left join fetch p.category c where c is null or c.slug = 'general'")
    java.util.List<Product> findUncategorized();
}
