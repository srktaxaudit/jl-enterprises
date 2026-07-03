package in.jlenterprises.ecommerce.dto.catalog;

import java.util.UUID;

public record ProductImageDto(
        UUID id,
        String url,
        String altText,
        int sortOrder,
        boolean primary
) {}
