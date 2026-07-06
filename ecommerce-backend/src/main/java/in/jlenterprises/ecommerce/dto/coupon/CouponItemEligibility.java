package in.jlenterprises.ecommerce.dto.coupon;

import java.math.BigDecimal;
import java.util.UUID;

/** Explains whether an individual cart line participates in a coupon. */
public record CouponItemEligibility(
        UUID productId,
        String productName,
        String categoryName,
        BigDecimal lineTotal,
        boolean eligible,
        String reason
) {}
