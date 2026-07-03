package in.jlenterprises.ecommerce.controller.catalog;

import in.jlenterprises.ecommerce.dto.catalog.CategoryDto;
import in.jlenterprises.ecommerce.request.catalog.CategoryRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.service.CategoryService;
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
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Product categories (public reads, admin writes)")
public class CategoryController {

    private static final String STAFF = "hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER')";

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List all categories")
    public ApiResponse<List<CategoryDto>> list() {
        return ApiResponse.success(categoryService.list());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a category by slug")
    public ApiResponse<CategoryDto> get(@PathVariable String slug) {
        return ApiResponse.success(categoryService.getBySlug(slug));
    }

    @PostMapping
    @PreAuthorize(STAFF)
    @Operation(summary = "Create a category")
    public ResponseEntity<ApiResponse<CategoryDto>> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created", categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(STAFF)
    @Operation(summary = "Update a category")
    public ApiResponse<CategoryDto> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("Category updated", categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(STAFF)
    @Operation(summary = "Delete a category")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ApiResponse.message("Category deleted");
    }
}
