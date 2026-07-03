package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.catalog.CategoryDto;
import in.jlenterprises.ecommerce.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    CategoryDto toDto(Category category);

    List<CategoryDto> toDtoList(List<Category> categories);
}
