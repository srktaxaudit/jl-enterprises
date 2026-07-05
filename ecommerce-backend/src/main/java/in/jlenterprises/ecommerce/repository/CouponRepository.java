package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    /**
     * Atomically bump used_count only while under the usage limit. Returns the number
     * of rows updated (1 = redeemed, 0 = limit already reached). This closes the
     * check-then-increment race where concurrent orders could exceed usageLimit.
     */
    @Modifying
    @Query("update Coupon c set c.usedCount = c.usedCount + 1 "
            + "where c.id = :id and (c.usageLimit is null or c.usedCount < c.usageLimit)")
    int incrementUsageIfAvailable(@Param("id") UUID id);
}
