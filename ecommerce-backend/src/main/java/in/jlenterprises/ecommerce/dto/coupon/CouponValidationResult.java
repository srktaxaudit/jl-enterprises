package in.jlenterprises.ecommerce.dto.coupon;

import java.math.BigDecimal;
import java.util.List;

/** Returned by the public validate endpoint when a coupon is applicable. */
public record CouponValidationResult(
        String code,
        BigDecimal discount,
        BigDecimal eligibleSubtotal,
        List<CouponItemEligibility> items
) {}
