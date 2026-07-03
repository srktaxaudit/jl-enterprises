package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.coupon.CouponDto;
import in.jlenterprises.ecommerce.entity.Coupon;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface CouponMapper {

    CouponDto toDto(Coupon coupon);

    List<CouponDto> toDtoList(List<Coupon> coupons);
}
