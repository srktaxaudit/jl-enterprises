package in.jlenterprises.ecommerce.controller.catalog;

import in.jlenterprises.ecommerce.dto.catalog.ProductDetailDto;
import in.jlenterprises.ecommerce.dto.catalog.ProductSearchCriteria;
import in.jlenterprises.ecommerce.dto.catalog.ProductSummaryDto;
import in.jlenterprises.ecommerce.request.catalog.ProductRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product catalog: search, detail, featured, related (admin writes)")
public class ProductController {

    private static final String STAFF = "hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER')";

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Search / list products with filters, sorting and pagination")
    public ApiResponse<PageResponse<ProductSummaryDto>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) BigDecimal minRating,
            @PageableDefault(size = 20) Pageable pageable) {
        var criteria = new ProductSearchCriteria(search, category, brand, minPrice, maxPrice, featured, minRating);
        return ApiResponse.success(PageResponse.of(productService.search(criteria, pageable)));
    }

    @GetMapping("/featured")
    @Operation(summary = "List featured products")
    public ApiResponse<PageResponse<ProductSummaryDto>> featured(@PageableDefault(size = 12) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(productService.featured(pageable)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get product detail by slug")
    public ApiResponse<ProductDetailDto> get(@PathVariable String slug) {
        return ApiResponse.success(productService.getBySlug(slug));
    }

    @GetMapping("/{slug}/related")
    @Operation(summary = "List products related to the given one")
    public ApiResponse<List<ProductSummaryDto>> related(@PathVariable String slug,
                                                        @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(productService.related(slug, limit));
    }

    @PostMapping
    @PreAuthorize(STAFF)
    @Operation(summary = "Create a product")
    public ResponseEntity<ApiResponse<ProductDetailDto>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", productService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(STAFF)
    @Operation(summary = "Update a product")
    public ApiResponse<ProductDetailDto> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("Product updated", productService.update(id, request));
    }

    @PatchMapping("/{id}/featured")
    @PreAuthorize(STAFF)
    @Operation(summary = "Toggle a product's featured flag")
    public ApiResponse<ProductDetailDto> setFeatured(@PathVariable UUID id, @RequestParam boolean featured) {
        return ApiResponse.success(productService.setFeatured(id, featured));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(STAFF)
    @Operation(summary = "Delete a product")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ApiResponse.message("Product deleted");
    }
}
