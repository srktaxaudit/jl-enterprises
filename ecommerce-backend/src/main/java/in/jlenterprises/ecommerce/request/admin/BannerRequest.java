package in.jlenterprises.ecommerce.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Create/update payload for a storefront banner. */
public record BannerRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 500) String imageUrl,
        @Size(max = 500) String linkUrl,
        @Size(max = 40) String position,
        int sortOrder,
        Boolean active,
        Instant startsAt,
        Instant endsAt
) {}
