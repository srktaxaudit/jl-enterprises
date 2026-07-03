package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.constant.ReviewStatus;
import in.jlenterprises.ecommerce.dto.review.ReviewDto;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER')")
@Tag(name = "Admin — Reviews", description = "Review moderation (staff)")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @Operation(summary = "List reviews by status (default PENDING)")
    public ApiResponse<PageResponse<ReviewDto>> list(
            @RequestParam(defaultValue = "PENDING") ReviewStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(reviewService.listByStatus(status, pageable)));
    }

    @PatchMapping("/{id}/moderate")
    @Operation(summary = "Approve or reject a review")
    public ApiResponse<ReviewDto> moderate(@PathVariable UUID id, @RequestParam ReviewStatus status) {
        return ApiResponse.success("Review " + status.name().toLowerCase(), reviewService.moderate(id, status));
    }
}
