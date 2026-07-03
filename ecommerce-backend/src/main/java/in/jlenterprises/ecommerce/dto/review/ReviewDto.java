package in.jlenterprises.ecommerce.dto.review;

import in.jlenterprises.ecommerce.constant.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record ReviewDto(
        UUID id,
        UUID productId,
        String reviewerName,
        int rating,
        String title,
        String comment,
        ReviewStatus status,
        boolean verifiedPurchase,
        Instant createdAt
) {}
