package in.jlenterprises.ecommerce.request.coupon;

import in.jlenterprises.ecommerce.constant.CouponType;
import in.jlenterprises.ecommerce.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CouponRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = ValidationPatterns.CODE, message = ValidationPatterns.MSG_CODE) String code,
        @Size(max = 120) @Pattern(regexp = ValidationPatterns.TITLE_OPT, message = ValidationPatterns.MSG_TITLE) String name,
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
        Boolean active,
        /** Null/empty means All Categories (also preserves legacy clients). */
        Set<UUID> categoryIds
) {}
