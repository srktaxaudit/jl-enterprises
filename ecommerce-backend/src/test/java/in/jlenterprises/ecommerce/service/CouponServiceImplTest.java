package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.CouponType;
import in.jlenterprises.ecommerce.entity.Coupon;
import in.jlenterprises.ecommerce.entity.CartItem;
import in.jlenterprises.ecommerce.entity.Category;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.mapper.CouponMapper;
import in.jlenterprises.ecommerce.repository.CouponRepository;
import in.jlenterprises.ecommerce.repository.CouponUsageRepository;
import in.jlenterprises.ecommerce.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock CouponRepository couponRepository;
    @Mock CouponUsageRepository couponUsageRepository;
    @Mock CouponMapper couponMapper;

    @InjectMocks CouponServiceImpl couponService;

    @Test
    void percentageCouponComputesCappedDiscount() {
        Coupon coupon = new Coupon();
        coupon.setCode("SAVE10");
        coupon.setType(CouponType.PERCENTAGE);
        coupon.setValue(new BigDecimal("10"));
        coupon.setActive(true);
        when(couponRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(coupon));

        var applied = couponService.apply("SAVE10", List.of(item("TV", "Televisions", "1000")), UUID.randomUUID());

        assertEquals(0, applied.discount().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void expiredCouponIsRejected() {
        Coupon coupon = new Coupon();
        coupon.setCode("OLD");
        coupon.setType(CouponType.FIXED);
        coupon.setValue(new BigDecimal("50"));
        coupon.setActive(true);
        coupon.setExpiresAt(Instant.now().minusSeconds(3600));
        when(couponRepository.findByCodeIgnoreCase("OLD")).thenReturn(Optional.of(coupon));

        assertThrows(BusinessException.class,
                () -> couponService.apply("OLD", List.of(item("TV", "Televisions", "1000")), UUID.randomUUID()));
    }

    @Test
    void unknownCodeIsRejected() {
        when(couponRepository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> couponService.apply("NOPE", List.of(item("TV", "Televisions", "1000")), UUID.randomUUID()));
    }

    @Test
    void categoryCouponDiscountsOnlyEligibleCartLines() {
        Category televisions = category("Televisions");
        Category refrigerators = category("Refrigerators");
        Coupon coupon = new Coupon();
        coupon.setCode("TV10");
        coupon.setType(CouponType.PERCENTAGE);
        coupon.setValue(new BigDecimal("10"));
        coupon.setActive(true);
        coupon.getApplicableCategories().add(televisions);
        when(couponRepository.findByCodeIgnoreCase("TV10")).thenReturn(Optional.of(coupon));

        var applied = couponService.apply("TV10", List.of(
                item("Smart TV", televisions, "50000"),
                item("Fridge", refrigerators, "30000")), UUID.randomUUID());

        assertEquals(0, applied.eligibleSubtotal().compareTo(new BigDecimal("50000")));
        assertEquals(0, applied.discount().compareTo(new BigDecimal("5000.00")));
        assertEquals(1, applied.items().stream().filter(i -> i.eligible()).count());
    }

    private static CartItem item(String productName, String categoryName, String price) {
        return item(productName, category(categoryName), price);
    }

    private static CartItem item(String productName, Category category, String price) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(productName);
        product.setCategory(category);
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal(price));
        return item;
    }

    private static Category category(String name) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        return category;
    }
}
