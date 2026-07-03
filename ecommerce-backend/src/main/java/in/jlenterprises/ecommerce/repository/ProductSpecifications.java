package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Composable {@link Specification}s for product search / filtering. The service
 * layer AND-combines the ones relevant to a request, keeping query-building out
 * of controllers and repositories (Specification pattern).
 */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    /** Case-insensitive match on name, short description or SKU. */
    public static Specification<Product> search(String term) {
        if (term == null || term.isBlank()) return null;
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate name = cb.like(cb.lower(root.get("name")), like);
            Predicate shortDesc = cb.like(cb.lower(root.get("shortDescription")), like);
            Predicate sku = cb.like(cb.lower(root.get("sku")), like);
            return cb.or(name, shortDesc, sku);
        };
    }

    public static Specification<Product> inCategorySlug(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.join("category").get("slug"), slug);
    }

    public static Specification<Product> inBrandSlug(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.join("brand").get("slug"), slug);
    }

    public static Specification<Product> priceGoe(BigDecimal min) {
        if (min == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceLoe(BigDecimal max) {
        if (max == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    public static Specification<Product> featured(Boolean featured) {
        if (featured == null) return null;
        return (root, query, cb) -> cb.equal(root.get("featured"), featured);
    }

    public static Specification<Product> minRating(BigDecimal rating) {
        if (rating == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("averageRating"), rating);
    }
}
