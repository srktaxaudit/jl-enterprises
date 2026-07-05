package in.jlenterprises.ecommerce.request.catalog;

import in.jlenterprises.ecommerce.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Create/update payload for a brand. */
public record BrandRequest(
        @NotBlank @Size(max = 120) @Pattern(regexp = ValidationPatterns.TITLE, message = ValidationPatterns.MSG_TITLE) String name,
        @Size(max = 140) String slug,
        @Size(max = 500) String logoUrl,
        String description
) {}
