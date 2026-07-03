package in.jlenterprises.ecommerce.dto.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductVariantDto(
        UUID id,
        String sku,
        String title,
        BigDecimal price,
        BigDecimal comparePrice,
        List<VariantOptionDto> options
) {}
