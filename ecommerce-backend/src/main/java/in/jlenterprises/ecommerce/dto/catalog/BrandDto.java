package in.jlenterprises.ecommerce.dto.catalog;

import java.util.UUID;

/** Read model for a brand. */
public record BrandDto(
        UUID id,
        String name,
        String slug,
        String logoUrl,
        String description
) {}
