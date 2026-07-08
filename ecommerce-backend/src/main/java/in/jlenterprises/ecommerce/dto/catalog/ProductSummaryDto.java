package in.jlenterprises.ecommerce.dto.catalog;

import java.math.BigDecimal;
import java.util.UUID;

/** Lightweight product projection for list/grid/search views. */
public record ProductSummaryDto(
        UUID id,
        String name,
        String slug,
        String sku,
        BigDecimal price,
        BigDecimal comparePrice,
        BigDecimal discountPercent,
        String currency,
        boolean featured,
        BigDecimal averageRating,
        int reviewCount,
        String primaryImageUrl,
        String brandName,
        String categorySlug,
        // ── EMI (for list/grid cards; manual only) ──
        boolean emiAvailable,
        BigDecimal emiAmount,
        Integer emiMonths
) {}
