package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.review.ReviewDto;
import in.jlenterprises.ecommerce.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface ReviewMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "reviewerName", source = "user.firstName")
    @Mapping(target = "status", source = "reviewStatus")
    ReviewDto toDto(Review review);
}
