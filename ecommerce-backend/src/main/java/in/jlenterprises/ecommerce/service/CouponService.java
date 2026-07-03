package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.coupon.CouponDto;
import in.jlenterprises.ecommerce.dto.coupon.CouponValidationResult;
import in.jlenterprises.ecommerce.entity.Coupon;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.request.coupon.CouponRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CouponService {

    List<CouponDto> list();

    CouponDto create(CouponRequest request);

    CouponDto update(UUID id, CouponRequest request);

    void delete(UUID id);

    /** Public check — throws if the coupon is not applicable to this subtotal/user. */
    CouponValidationResult validate(String code, BigDecimal subtotal, UUID userId);

    /** Validate and return the entity + computed discount, for order placement. */
    AppliedCoupon apply(String code, BigDecimal subtotal, UUID userId);

    /** Persist a redemption and bump the usage counter (called after the order is saved). */
    void recordUsage(AppliedCoupon applied, User user, Order order);

    /** Internal holder for a validated coupon and the discount it yields. */
    record AppliedCoupon(Coupon coupon, BigDecimal discount) {}
}
