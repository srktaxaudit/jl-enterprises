package in.jlenterprises.ecommerce.request.coupon;

import in.jlenterprises.ecommerce.constant.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponRequest(
        @NotBlank @Size(max = 40) String code,
        @Size(max = 120) String name,
        @Size(max = 200) String description,
        @NotNull CouponType type,
        @NotNull @Positive BigDecimal value,
        @PositiveOrZero BigDecimal minOrderAmount,
        @PositiveOrZero BigDecimal maxDiscount,
        Integer usageLimit,
        Integer perUserLimit,
        Boolean firstOrderOnly,
        Instant startsAt,
        Instant expiresAt,
        Boolean active
) {}
