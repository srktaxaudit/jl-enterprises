package in.jlenterprises.ecommerce.controller.catalog;

import in.jlenterprises.ecommerce.dto.catalog.BrandDto;
import in.jlenterprises.ecommerce.request.catalog.BrandRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands")
@Tag(name = "Brands", description = "Product brands (public reads, admin writes)")
public class BrandController {

    private static final String STAFF = "hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER','PRODUCT_MANAGER')";

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    @Operation(summary = "List all brands")
    public ApiResponse<List<BrandDto>> list() {
        return ApiResponse.success(brandService.list());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a brand by slug")
    public ApiResponse<BrandDto> get(@PathVariable String slug) {
        return ApiResponse.success(brandService.getBySlug(slug));
    }

    @PostMapping
    @PreAuthorize(STAFF)
    @Operation(summary = "Create a brand")
    public ResponseEntity<ApiResponse<BrandDto>> create(@Valid @RequestBody BrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Brand created", brandService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(STAFF)
    @Operation(summary = "Update a brand")
    public ApiResponse<BrandDto> update(@PathVariable UUID id, @Valid @RequestBody BrandRequest request) {
        return ApiResponse.success("Brand updated", brandService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(STAFF)
    @Operation(summary = "Delete a brand")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        brandService.delete(id);
        return ApiResponse.message("Brand deleted");
    }
}
