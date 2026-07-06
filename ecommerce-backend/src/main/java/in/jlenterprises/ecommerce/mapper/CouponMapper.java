package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.coupon.CouponDto;
import in.jlenterprises.ecommerce.dto.coupon.CategoryTargetDto;
import in.jlenterprises.ecommerce.entity.Coupon;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface CouponMapper {

    default CouponDto toDto(Coupon coupon) {
        var categories = coupon.getApplicableCategories().stream()
                .map(c -> new CategoryTargetDto(c.getId(), c.getName(), c.getSlug()))
                .sorted(java.util.Comparator.comparing(CategoryTargetDto::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new CouponDto(coupon.getId(), coupon.getCode(), coupon.getName(), coupon.getDescription(),
                coupon.getType(), coupon.getValue(), coupon.getMinOrderAmount(), coupon.getMaxDiscount(),
                coupon.getUsageLimit(), coupon.getUsedCount(), coupon.getPerUserLimit(), coupon.isFirstOrderOnly(),
                coupon.getStartsAt(), coupon.getExpiresAt(), coupon.isActive(), categories.isEmpty(), categories);
    }

    default List<CouponDto> toDtoList(List<Coupon> coupons) {
        return coupons.stream().map(this::toDto).toList();
    }
}
