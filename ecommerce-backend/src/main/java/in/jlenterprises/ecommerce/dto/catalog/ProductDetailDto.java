package in.jlenterprises.ecommerce.dto.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Full product view for the product detail page. */
public record ProductDetailDto(
        UUID id,
        String name,
        String slug,
        String sku,
        String shortDescription,
        String description,
        BigDecimal price,
        BigDecimal comparePrice,
        BigDecimal discountPercent,
        String currency,
        boolean featured,
        String metaTitle,
        String metaDescription,
        String specifications,
        BigDecimal averageRating,
        int reviewCount,
        long viewCount,
        long salesCount,
        Integer availableStock,
        String primaryImageUrl,
        UUID categoryId,
        String categorySlug,
        UUID brandId,
        String brandName,
        List<ProductImageDto> images,
        List<ProductVariantDto> variants,
        // ── GST (per product; null rate → store default) ──
        BigDecimal gstRate,
        String hsnCode,
        // ── EMI (manually set by admin; storefront shows these as-is) ──
        boolean emiAvailable,
        Integer emiMonths,
        BigDecimal emiAmount,
        BigDecimal emiDownPayment,
        BigDecimal emiProcessingFee,
        String emiNote
) {}
