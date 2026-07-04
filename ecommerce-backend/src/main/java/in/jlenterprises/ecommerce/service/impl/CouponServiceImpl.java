package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.CouponType;
import in.jlenterprises.ecommerce.dto.coupon.CouponDto;
import in.jlenterprises.ecommerce.dto.coupon.CouponValidationResult;
import in.jlenterprises.ecommerce.entity.Coupon;
import in.jlenterprises.ecommerce.entity.CouponUsage;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.DuplicateResourceException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.CouponMapper;
import in.jlenterprises.ecommerce.repository.CouponRepository;
import in.jlenterprises.ecommerce.repository.CouponUsageRepository;
import in.jlenterprises.ecommerce.request.coupon.CouponRequest;
import in.jlenterprises.ecommerce.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponMapper couponMapper;

    public CouponServiceImpl(CouponRepository couponRepository, CouponUsageRepository couponUsageRepository,
                             CouponMapper couponMapper) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.couponMapper = couponMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponDto> list() {
        return couponMapper.toDtoList(couponRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponDto> activePublic() {
        Instant now = Instant.now();
        return couponRepository.findAll().stream()
                .filter(Coupon::isActive)
                .filter(c -> c.getStartsAt() == null || !now.isBefore(c.getStartsAt()))
                .filter(c -> c.getExpiresAt() == null || !now.isAfter(c.getExpiresAt()))
                .filter(c -> c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit())
                .map(couponMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CouponDto create(CouponRequest request) {
        String code = request.code().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Coupon code already exists: " + code);
        }
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        apply(coupon, request);
        return couponMapper.toDto(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponDto update(UUID id, CouponRequest request) {
        Coupon coupon = getEntity(id);
        apply(coupon, request);
        return couponMapper.toDto(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Coupon coupon = getEntity(id);
        coupon.setDeleted(true);
        couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResult validate(String code, BigDecimal subtotal, UUID userId) {
        AppliedCoupon applied = apply(code, subtotal, userId);
        return new CouponValidationResult(applied.coupon().getCode(), applied.discount());
    }

    @Override
    @Transactional(readOnly = true)
    public AppliedCoupon apply(String code, BigDecimal subtotal, UUID userId) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException("Invalid coupon code"));

        Instant now = Instant.now();
        if (!coupon.isActive()) throw new BusinessException("Coupon is not active");
        if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt()))
            throw new BusinessException("Coupon is not yet valid");
        if (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt()))
            throw new BusinessException("Coupon has expired");
        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0)
            throw new BusinessException("Order total below the minimum for this coupon");
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit())
            throw new BusinessException("Coupon usage limit reached");
        if (coupon.getPerUserLimit() != null
                && couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId) >= coupon.getPerUserLimit())
            throw new BusinessException("You have already used this coupon the maximum number of times");

        return new AppliedCoupon(coupon, computeDiscount(coupon, subtotal));
    }

    @Override
    @Transactional
    public void recordUsage(AppliedCoupon applied, User user, Order order) {
        Coupon coupon = applied.coupon();
        CouponUsage usage = new CouponUsage();
        usage.setCoupon(coupon);
        usage.setUser(user);
        usage.setOrder(order);
        usage.setDiscountApplied(applied.discount());
        couponUsageRepository.save(usage);

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
    }

    // ── helpers ──
    private BigDecimal computeDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount;
        if (coupon.getType() == CouponType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getValue();
        }
        // Never discount more than the subtotal.
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private void apply(Coupon coupon, CouponRequest r) {
        coupon.setDescription(r.description());
        coupon.setType(r.type());
        coupon.setValue(r.value());
        coupon.setMinOrderAmount(r.minOrderAmount());
        coupon.setMaxDiscount(r.maxDiscount());
        coupon.setUsageLimit(r.usageLimit());
        coupon.setPerUserLimit(r.perUserLimit());
        coupon.setStartsAt(r.startsAt());
        coupon.setExpiresAt(r.expiresAt());
        if (r.active() != null) coupon.setActive(r.active());
    }

    private Coupon getEntity(UUID id) {
        return couponRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Coupon", id));
    }
}
