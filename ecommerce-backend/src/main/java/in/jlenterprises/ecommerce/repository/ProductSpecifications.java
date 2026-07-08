package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.RecordStatus;
import in.jlenterprises.ecommerce.entity.Inventory;
import in.jlenterprises.ecommerce.entity.Product;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

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

    /** Filter by "EMI available" flag (admin catalog filter). */
    public static Specification<Product> emiAvailable(Boolean emiAvailable) {
        if (emiAvailable == null) return null;
        return (root, query, cb) -> cb.equal(root.get("emiAvailable"), emiAvailable);
    }

    /**
     * Stock filter: inStock=true → has an inventory record with available (quantity − reserved)
     * &gt; 0; inStock=false → out of stock (no record, or available ≤ 0). Mirrors {@link #visible()}'s
     * stock sub-query but without the ACTIVE requirement, so admins can filter the full catalogue.
     */
    public static Specification<Product> inStock(Boolean inStock) {
        if (inStock == null) return null;
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<Inventory> inv = sub.from(Inventory.class);
            sub.select(inv.get("id"));
            sub.where(
                    cb.equal(inv.get("product"), root),
                    cb.greaterThan(cb.diff(inv.<Integer>get("quantity"), inv.<Integer>get("reserved")), 0));
            return inStock ? cb.exists(sub) : cb.not(cb.exists(sub));
        };
    }

    /**
     * Storefront visibility: the product is ACTIVE and has an inventory record with
     * available stock (quantity − reserved) &gt; 0. Products that are inactive, have
     * no inventory, or are out of stock are excluded from customer-facing listings.
     */
    public static Specification<Product> visible() {
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<Inventory> inv = sub.from(Inventory.class);
            sub.select(inv.get("id"));
            sub.where(
                    cb.equal(inv.get("product"), root),
                    cb.greaterThan(cb.diff(inv.<Integer>get("quantity"), inv.<Integer>get("reserved")), 0));
            return cb.and(cb.equal(root.get("status"), RecordStatus.ACTIVE), cb.exists(sub));
        };
    }
}
