package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.catalog.BrandDto;
import in.jlenterprises.ecommerce.request.catalog.BrandRequest;

import java.util.List;
import java.util.UUID;

public interface BrandService {

    List<BrandDto> list();

    BrandDto getBySlug(String slug);

    BrandDto create(BrandRequest request);

    BrandDto update(UUID id, BrandRequest request);

    void delete(UUID id);
}
