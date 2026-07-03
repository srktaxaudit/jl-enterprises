package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.ReviewStatus;
import in.jlenterprises.ecommerce.dto.review.ReviewDto;
import in.jlenterprises.ecommerce.request.review.ReviewRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    ReviewDto create(UUID userId, UUID productId, ReviewRequest request);

    Page<ReviewDto> listApproved(UUID productId, Pageable pageable);

    Page<ReviewDto> listByStatus(ReviewStatus status, Pageable pageable);

    ReviewDto moderate(UUID reviewId, ReviewStatus status);
}
