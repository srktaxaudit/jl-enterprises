package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.catalog.CategoryDto;
import in.jlenterprises.ecommerce.request.catalog.CategoryRequest;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    List<CategoryDto> list();

    CategoryDto getBySlug(String slug);

    CategoryDto create(CategoryRequest request);

    CategoryDto update(UUID id, CategoryRequest request);

    void delete(UUID id);
}
