package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.catalog.ProductDetailDto;
import in.jlenterprises.ecommerce.dto.catalog.ProductSearchCriteria;
import in.jlenterprises.ecommerce.dto.catalog.ProductSummaryDto;
import in.jlenterprises.ecommerce.request.catalog.ProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    /** Paged search with dynamic filtering + sorting. */
    Page<ProductSummaryDto> search(ProductSearchCriteria criteria, Pageable pageable);

    /** Full detail by slug; increments the view counter. */
    ProductDetailDto getBySlug(String slug);

    Page<ProductSummaryDto> featured(Pageable pageable);

    List<ProductSummaryDto> related(String slug, int limit);

    ProductDetailDto create(ProductRequest request);

    ProductDetailDto update(UUID id, ProductRequest request);

    ProductDetailDto setFeatured(UUID id, boolean featured);

    void delete(UUID id);
}
