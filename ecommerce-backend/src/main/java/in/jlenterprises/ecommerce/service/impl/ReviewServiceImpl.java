package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.constant.ReviewStatus;
import in.jlenterprises.ecommerce.dto.review.ReviewDto;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.entity.Review;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.DuplicateResourceException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.ReviewMapper;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import in.jlenterprises.ecommerce.repository.ReviewRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.request.review.ReviewRequest;
import in.jlenterprises.ecommerce.service.NotificationService;
import in.jlenterprises.ecommerce.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final NotificationService notificationService;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ProductRepository productRepository,
                             UserRepository userRepository, ReviewMapper reviewMapper,
                             NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public ReviewDto create(UUID userId, UUID productId, ReviewRequest request) {
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateResourceException("You have already reviewed this product");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setComment(request.comment());
        review.setReviewStatus(ReviewStatus.PENDING);   // moderated before publishing
        Review saved = reviewRepository.save(review);
        String name = user.getFullName();
        notificationService.notifyAdmins(NotificationType.REVIEW, "New review",
                "New review from " + (name == null || name.isBlank() ? user.getEmail() : name)
                        + " on " + product.getName() + " (awaiting moderation).",
                "/admin-reviews.html", "Reviews", saved.getId(), "REVIEW");
        return reviewMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDto> listApproved(UUID productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndReviewStatus(productId, ReviewStatus.APPROVED, pageable)
                .map(reviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDto> listByStatus(ReviewStatus status, Pageable pageable) {
        return reviewRepository.findByReviewStatus(status, pageable).map(reviewMapper::toDto);
    }

    @Override
    @Transactional
    public ReviewDto moderate(UUID reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.of("Review", reviewId));
        review.setReviewStatus(status);
        reviewRepository.save(review);
        recomputeProductRating(review.getProduct());
        return reviewMapper.toDto(review);
    }

    /** Refresh the product's denormalised rating/count from its APPROVED reviews. */
    private void recomputeProductRating(Product product) {
        UUID productId = product.getId();
        double avg = reviewRepository.averageRating(productId);
        long count = reviewRepository.approvedCount(productId);
        product.setAverageRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        product.setReviewCount((int) count);
        productRepository.save(product);
    }
}
