package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.ReviewStatus;
import in.jlenterprises.ecommerce.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProductIdAndReviewStatus(UUID productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByReviewStatus(ReviewStatus status, Pageable pageable);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    @Query("select coalesce(avg(r.rating), 0) from Review r " +
           "where r.product.id = :productId and r.reviewStatus = in.jlenterprises.ecommerce.constant.ReviewStatus.APPROVED")
    double averageRating(@Param("productId") UUID productId);

    @Query("select count(r) from Review r " +
           "where r.product.id = :productId and r.reviewStatus = in.jlenterprises.ecommerce.constant.ReviewStatus.APPROVED")
    long approvedCount(@Param("productId") UUID productId);
}
