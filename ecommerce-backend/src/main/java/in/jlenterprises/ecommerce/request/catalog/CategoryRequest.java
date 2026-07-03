package in.jlenterprises.ecommerce.request.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Create/update payload for a category. */
public record CategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 140) String slug,
        String description,
        @Size(max = 500) String imageUrl,
        Integer sortOrder,
        UUID parentId
) {}
