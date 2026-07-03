package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.catalog.BrandDto;
import in.jlenterprises.ecommerce.entity.Brand;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface BrandMapper {

    BrandDto toDto(Brand brand);

    List<BrandDto> toDtoList(List<Brand> brands);
}
