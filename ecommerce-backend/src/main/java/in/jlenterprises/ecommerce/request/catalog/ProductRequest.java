package in.jlenterprises.ecommerce.request.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** Create/update payload for a product (variants and images managed separately). */
public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 220) String slug,
        @NotBlank @Size(max = 80) String sku,
        @Size(max = 500) String shortDescription,
        String description,
        UUID categoryId,
        UUID brandId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @PositiveOrZero BigDecimal comparePrice,
        @PositiveOrZero BigDecimal discountPercent,
        @Size(min = 3, max = 3) String currency,
        Boolean featured,
        @Size(max = 200) String metaTitle,
        @Size(max = 300) String metaDescription,
        @Size(max = 4000) String specifications,
        // ── EMI (all optional; only used when emiAvailable is true) ──
        Boolean emiAvailable,
        @PositiveOrZero Integer emiMonths,
        @PositiveOrZero BigDecimal emiAmount,
        @PositiveOrZero BigDecimal emiDownPayment,
        @PositiveOrZero BigDecimal emiProcessingFee,
        @Size(max = 300) String emiNote
) {}
