package in.jlenterprises.ecommerce.request.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create/update payload for a brand. */
public record BrandRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 140) String slug,
        @Size(max = 500) String logoUrl,
        String description
) {}
