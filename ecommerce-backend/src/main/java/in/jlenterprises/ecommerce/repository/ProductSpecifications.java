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

    /** Separators that count as a word boundary in a product name/SKU (before the term). */
    private static final String[] WORD_BOUNDARIES = {" ", "-", "(", "/", ".", ",", "+", "_"};

    /**
     * Case-insensitive search on name, short description or SKU, matched at WORD BOUNDARIES
     * (a word must start with the term) rather than as an arbitrary substring. So "ac" matches
     * "…3STAR AC" / "ACGEI" / "ONIDA -AC-…" but NOT "m<u>ac</u>hine" or "r<u>ac</u>er". A word boundary
     * is the start of the value or one of {@link #WORD_BOUNDARIES}. Wildcards in the term are
     * escaped ('!'). Uses plain LIKE (no DB-specific functions).
     */
    public static Specification<Product> search(String term) {
        if (term == null || term.isBlank()) return null;
        String t = term.trim().toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return (root, query, cb) -> cb.or(
                wordStartLike(cb, root.get("name"), t),
                wordStartLike(cb, root.get("shortDescription"), t),
                wordStartLike(cb, root.get("sku"), t));
    }

    /** True when a word in {@code field} starts with {@code term}: matches "term%" (start of
        value) OR "%<sep>term%" for each word-boundary separator. Escape char '!'. */
    private static Predicate wordStartLike(jakarta.persistence.criteria.CriteriaBuilder cb,
                                           jakarta.persistence.criteria.Expression<String> field, String term) {
        jakarta.persistence.criteria.Expression<String> lower = cb.lower(field);
        java.util.List<Predicate> ors = new java.util.ArrayList<>();
        ors.add(cb.like(lower, term + "%", '!'));                         // starts with the term
        for (String sep : WORD_BOUNDARIES) {
            ors.add(cb.like(lower, "%" + sep + term + "%", '!'));         // term after a separator
        }
        return cb.or(ors.toArray(new Predicate[0]));
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
