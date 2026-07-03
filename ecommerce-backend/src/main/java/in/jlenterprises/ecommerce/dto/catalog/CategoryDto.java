package in.jlenterprises.ecommerce.dto.catalog;

import java.util.UUID;

/** Read model for a category. */
public record CategoryDto(
        UUID id,
        String name,
        String slug,
        String description,
        String imageUrl,
        int sortOrder,
        UUID parentId
) {}
