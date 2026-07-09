package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {

    Optional<Brand> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Find a brand by slug INCLUDING soft-deleted rows (native query bypasses
     * {@code @SQLRestriction}). The slug column is uniquely constrained regardless of the
     * soft-delete flag, so the auto-brand assigner must reuse/revive an existing row rather
     * than insert a duplicate slug (which would fail with a constraint violation).
     */
    @org.springframework.data.jpa.repository.Query(
            value = "select * from brands where slug = :slug limit 1", nativeQuery = true)
    Optional<Brand> findAnyBySlug(@org.springframework.data.repository.query.Param("slug") String slug);
}
