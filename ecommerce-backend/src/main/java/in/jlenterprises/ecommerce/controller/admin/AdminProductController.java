package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.dto.catalog.ProductSearchCriteria;
import in.jlenterprises.ecommerce.dto.catalog.ProductSummaryDto;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin catalog listing — returns ALL products (including inactive and out-of-stock),
 * unlike the public /api/v1/products endpoint which only lists visible ones. Product
 * create/update/delete + image endpoints remain on the shared /api/v1/products controller.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER')")
@Tag(name = "Admin — Products", description = "Full catalog listing for staff (all statuses/stock)")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List all products (admin) — includes inactive and out-of-stock")
    public ApiResponse<PageResponse<ProductSummaryDto>> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        var criteria = new ProductSearchCriteria(search, null, null, null, null, null, null);
        return ApiResponse.success(PageResponse.of(productService.search(criteria, pageable)));
    }
}
