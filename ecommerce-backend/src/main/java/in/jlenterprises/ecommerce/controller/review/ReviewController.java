package in.jlenterprises.ecommerce.controller.review;

import in.jlenterprises.ecommerce.dto.review.ReviewDto;
import in.jlenterprises.ecommerce.request.review.ReviewRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import in.jlenterprises.ecommerce.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@Tag(name = "Reviews", description = "Product reviews (public reads, customer writes)")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @Operation(summary = "List approved reviews for a product")
    public ApiResponse<PageResponse<ReviewDto>> list(
            @PathVariable UUID productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(reviewService.listApproved(productId, pageable)));
    }

    @PostMapping
    @Operation(summary = "Submit a review (goes to moderation)")
    public ResponseEntity<ApiResponse<ReviewDto>> create(@PathVariable UUID productId,
                                                        @Valid @RequestBody ReviewRequest request) {
        ReviewDto dto = reviewService.create(SecurityUtils.currentUserId(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted for approval", dto));
    }
}
