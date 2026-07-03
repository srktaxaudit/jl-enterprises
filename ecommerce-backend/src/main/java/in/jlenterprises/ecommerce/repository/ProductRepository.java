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

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    Page<Product> findByFeaturedTrue(Pageable pageable);

    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);
}
