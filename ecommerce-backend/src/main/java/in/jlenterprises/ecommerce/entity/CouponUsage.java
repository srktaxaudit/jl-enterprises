package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/** Records each redemption of a coupon (for per-user limit enforcement + reporting). */
@Entity
@Table(name = "coupon_usages", indexes = {
        @Index(name = "idx_usage_coupon", columnList = "coupon_id"),
        @Index(name = "idx_usage_user", columnList = "user_id")
})
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class CouponUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_usage_coupon"))
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_usage_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_usage_order"))
    private Order order;

    @Column(name = "discount_applied", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountApplied;
}
