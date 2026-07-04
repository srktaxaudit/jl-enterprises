package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.CouponType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;

/** A discount coupon with validity window and usage limits. */
@Entity
@Table(name = "coupons", uniqueConstraints = @UniqueConstraint(name = "uk_coupon_code", columnNames = "code"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Coupon extends BaseEntity {

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", length = 120)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    /** When true, the coupon is valid only on a customer's very first order.
        columnDefinition sets a DB default so auto-schema-update can add this NOT NULL
        column to a coupons table that already has rows. */
    @Column(name = "first_order_only", nullable = false, columnDefinition = "boolean not null default false")
    private boolean firstOrderOnly = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CouponType type;

    /** Percentage (0–100) when type=PERCENTAGE, otherwise a flat amount. */
    @Column(name = "value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_discount", precision = 12, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
