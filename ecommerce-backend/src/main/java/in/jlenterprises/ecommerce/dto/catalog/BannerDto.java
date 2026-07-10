package in.jlenterprises.ecommerce.dto.catalog;

import java.time.Instant;
import java.util.UUID;

/** A storefront promotional banner. */
public record BannerDto(
        UUID id,
        String title,
        String imageUrl,
        String linkUrl,
        String position,
        int sortOrder,
        boolean active,
        Instant startsAt,
        Instant endsAt
) {}
