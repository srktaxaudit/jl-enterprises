package in.jlenterprises.ecommerce.dto.catalog;

import java.math.BigDecimal;

/** Filter parameters for product search (all optional; nulls are ignored). */
public record ProductSearchCriteria(
        String search,
        String categorySlug,
        String brandSlug,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean featured,
        BigDecimal minRating
) {}
