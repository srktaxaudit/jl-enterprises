package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.CouponType;
import in.jlenterprises.ecommerce.dto.coupon.CouponDto;
import in.jlenterprises.ecommerce.dto.coupon.CouponItemEligibility;
import in.jlenterprises.ecommerce.dto.coupon.CouponValidationResult;
import in.jlenterprises.ecommerce.entity.Cart;
import in.jlenterprises.ecommerce.entity.CartItem;
import in.jlenterprises.ecommerce.entity.Category;
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
import in.jlenterprises.ecommerce.repository.CartRepository;
import in.jlenterprises.ecommerce.repository.CategoryRepository;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.request.coupon.CouponRequest;
import in.jlenterprises.ecommerce.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CategoryRepository categoryRepository;
    private final CouponMapper couponMapper;

    public CouponServiceImpl(CouponRepository couponRepository, CouponUsageRepository couponUsageRepository,
                             OrderRepository orderRepository, CartRepository cartRepository,
                             CategoryRepository categoryRepository, CouponMapper couponMapper) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.categoryRepository = categoryRepository;
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
    @Auditable(action = "CREATE_COUPON", entity = "coupon")
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
    @Auditable(action = "UPDATE_COUPON", entity = "coupon")
    public CouponDto update(UUID id, CouponRequest request) {
        Coupon coupon = getEntity(id);
        apply(coupon, request);
        return couponMapper.toDto(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_COUPON", entity = "coupon")
    public void delete(UUID id) {
        Coupon coupon = getEntity(id);
        coupon.setDeleted(true);
        couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponDto> eligibleFor(UUID userId) {
        // Reuse the full validation: a coupon is eligible only if apply() succeeds
        // for this user + subtotal (covers per-user limit, first-order, min-order, etc.).
        List<CouponDto> eligible = new ArrayList<>();
        List<CartItem> items = currentCartItems(userId);
        for (CouponDto c : activePublic()) {
            try {
                apply(c.code(), items, userId);
                eligible.add(c);
            } catch (RuntimeException ignored) {
                // not eligible for this user right now — skip it
            }
        }
        return eligible;
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResult validate(String code, UUID userId) {
        AppliedCoupon applied = apply(code, currentCartItems(userId), userId);
        return new CouponValidationResult(applied.coupon().getCode(), applied.discount(),
                applied.eligibleSubtotal(), applied.items());
    }

    @Override
    @Transactional(readOnly = true)
    public AppliedCoupon apply(String code, List<CartItem> items, UUID userId) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException("Invalid coupon code"));

        List<CouponItemEligibility> itemResults = evaluateItems(coupon, items);
        BigDecimal eligibleSubtotal = itemResults.stream()
                .filter(CouponItemEligibility::eligible)
                .map(CouponItemEligibility::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Instant now = Instant.now();
        if (!coupon.isActive()) throw new BusinessException("Coupon is not active");
        if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt()))
            throw new BusinessException("Coupon is not yet valid");
        if (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt()))
            throw new BusinessException("Coupon has expired");
        if (eligibleSubtotal.signum() <= 0)
            throw new BusinessException("This coupon does not apply to any product in your cart");
        if (coupon.getMinOrderAmount() != null && eligibleSubtotal.compareTo(coupon.getMinOrderAmount()) < 0)
            throw new BusinessException("Eligible products total is below the minimum for this coupon");
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit())
            throw new BusinessException("Coupon usage limit reached");
        if (coupon.getPerUserLimit() != null
                && couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId) >= coupon.getPerUserLimit())
            throw new BusinessException("You have already used this coupon.");
        if (coupon.isFirstOrderOnly() && orderRepository.countByUserId(userId) > 0)
            throw new BusinessException("This coupon is valid only on your first order.");

        return new AppliedCoupon(coupon, computeDiscount(coupon, eligibleSubtotal), eligibleSubtotal, itemResults);
    }

    @Override
    @Transactional
    public void recordUsage(AppliedCoupon applied, User user, Order order) {
        Coupon coupon = applied.coupon();

        // Atomically claim one use under the usage limit. If it returns 0 the limit
        // was reached by a concurrent order — fail so the whole placeOrder transaction
        // rolls back (stock is restored) rather than over-redeeming the coupon.
        if (couponRepository.incrementUsageIfAvailable(coupon.getId()) == 0) {
            throw new BusinessException("Coupon usage limit reached");
        }

        CouponUsage usage = new CouponUsage();
        usage.setCoupon(coupon);
        usage.setUser(user);
        usage.setOrder(order);
        usage.setDiscountApplied(applied.discount());
        couponUsageRepository.save(usage);
    }

    @Override
    @Transactional
    public void revokeForOrder(UUID orderId) {
        couponUsageRepository.findByOrderId(orderId).ifPresent(usage -> {
            Coupon coupon = usage.getCoupon();
            if (coupon != null && coupon.getUsedCount() > 0) {
                coupon.setUsedCount(coupon.getUsedCount() - 1);
                couponRepository.save(coupon);
            }
            couponUsageRepository.delete(usage);
        });
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
        coupon.setName(r.name());
        coupon.setDescription(r.description());
        coupon.setType(r.type());
        coupon.setValue(r.value());
        coupon.setMinOrderAmount(r.minOrderAmount());
        coupon.setMaxDiscount(r.maxDiscount());
        coupon.setUsageLimit(r.usageLimit());
        coupon.setPerUserLimit(r.perUserLimit());
        coupon.setFirstOrderOnly(r.firstOrderOnly() != null && r.firstOrderOnly());
        coupon.setStartsAt(r.startsAt());
        coupon.setExpiresAt(r.expiresAt());
        if (r.active() != null) coupon.setActive(r.active());
        coupon.getApplicableCategories().clear();
        if (r.categoryIds() != null && !r.categoryIds().isEmpty()) {
            Set<UUID> requested = new HashSet<>(r.categoryIds());
            List<Category> categories = categoryRepository.findAllById(requested);
            if (categories.size() != requested.size()) {
                throw new BusinessException("One or more selected categories no longer exist");
            }
            coupon.getApplicableCategories().addAll(categories);
        }
    }

    private List<CartItem> currentCartItems(UUID userId) {
        return cartRepository.findByUserId(userId).map(Cart::getItems).orElseGet(List::of);
    }

    private List<CouponItemEligibility> evaluateItems(Coupon coupon, List<CartItem> items) {
        boolean global = coupon.getApplicableCategories().isEmpty();
        Set<UUID> categoryIds = coupon.getApplicableCategories().stream().map(Category::getId).collect(java.util.stream.Collectors.toSet());
        return items.stream().map(item -> {
            Category category = item.getProduct().getCategory();
            boolean eligible = global || categoryMatches(category, categoryIds);
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            String categoryName = category == null ? "Uncategorised" : category.getName();
            String reason = eligible
                    ? (global ? "Coupon applies to all categories" : "Product belongs to an eligible category")
                    : "Category " + categoryName + " is not included in this coupon";
            return new CouponItemEligibility(item.getProduct().getId(), item.getProduct().getName(),
                    categoryName, lineTotal, eligible, reason);
        }).toList();
    }

    /** Selecting a parent category also includes its descendants. */
    private boolean categoryMatches(Category category, Set<UUID> selectedIds) {
        Category cursor = category;
        while (cursor != null) {
            if (selectedIds.contains(cursor.getId())) return true;
            cursor = cursor.getParent();
        }
        return false;
    }

    private Coupon getEntity(UUID id) {
        return couponRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Coupon", id));
    }
}
