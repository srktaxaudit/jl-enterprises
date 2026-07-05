package in.jlenterprises.ecommerce.request.catalog;

import in.jlenterprises.ecommerce.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Create/update payload for a category. */
public record CategoryRequest(
        @NotBlank @Size(max = 120) @Pattern(regexp = ValidationPatterns.TITLE, message = ValidationPatterns.MSG_TITLE) String name,
        @Size(max = 140) String slug,
        String description,
        @Size(max = 500) String imageUrl,
        Integer sortOrder,
        UUID parentId
) {}
