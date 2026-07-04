package in.jlenterprises.ecommerce.dto.coupon;

import in.jlenterprises.ecommerce.constant.CouponType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponDto(
        UUID id,
        String code,
        String name,
        String description,
        CouponType type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscount,
        Integer usageLimit,
        int usedCount,
        Integer perUserLimit,
        boolean firstOrderOnly,
        Instant startsAt,
        Instant expiresAt,
        boolean active
) {}
