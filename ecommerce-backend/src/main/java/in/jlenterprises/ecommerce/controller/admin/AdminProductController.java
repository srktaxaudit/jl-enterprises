package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.dto.admin.BulkUpdateResult;
import in.jlenterprises.ecommerce.dto.catalog.ProductSearchCriteria;
import in.jlenterprises.ecommerce.dto.catalog.ProductSummaryDto;
import in.jlenterprises.ecommerce.request.admin.ProductBulkRow;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.ProductCategorizerService;
import in.jlenterprises.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import java.util.Map;

/**
 * Admin catalog listing — returns ALL products (including inactive and out-of-stock),
 * unlike the public /api/v1/products endpoint which only lists visible ones. Product
 * create/update/delete + image endpoints remain on the shared /api/v1/products controller.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER','PRODUCT_MANAGER','INVENTORY_MANAGER')")
@Tag(name = "Admin — Products", description = "Full catalog listing for staff (all statuses/stock)")
public class AdminProductController {

    private final ProductService productService;
    private final ProductCategorizerService categorizerService;
    private final in.jlenterprises.ecommerce.service.ProductBrandAssignerService brandAssignerService;

    public AdminProductController(ProductService productService, ProductCategorizerService categorizerService,
                                  in.jlenterprises.ecommerce.service.ProductBrandAssignerService brandAssignerService) {
        this.productService = productService;
        this.categorizerService = categorizerService;
        this.brandAssignerService = brandAssignerService;
    }

    @GetMapping
    @Operation(summary = "List all products (admin) — includes inactive and out-of-stock; optional filters")
    public ApiResponse<PageResponse<ProductSummaryDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean emiAvailable,
            @PageableDefault(size = 50) Pageable pageable) {
        var criteria = new ProductSearchCriteria(search, category, null, null, null, null, null, inStock, emiAvailable);
        return ApiResponse.success(PageResponse.of(productService.search(criteria, pageable)));
    }

    @PostMapping("/auto-categorize")
    @Operation(summary = "Auto-file General/uncategorised products into the right department by name")
    public ApiResponse<Map<String, Integer>> autoCategorize() {
        Map<String, Integer> moved = categorizerService.autoCategorize();
        int total = moved.values().stream().mapToInt(Integer::intValue).sum();
        return ApiResponse.success("Organised " + total + " products.", moved);
    }

    @PostMapping("/auto-brands")
    @Operation(summary = "Assign a brand (inferred from the product name) to products that have none")
    public ApiResponse<Map<String, Integer>> autoAssignBrands() {
        Map<String, Integer> assigned = brandAssignerService.autoAssign();
        int total = assigned.values().stream().mapToInt(Integer::intValue).sum();
        return ApiResponse.success("Assigned brands to " + total + " products.", assigned);
    }

    @GetMapping("/export")
    @Operation(summary = "Export all products as CSV")
    public ResponseEntity<String> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"products.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(productService.exportProductsCsv());
    }

    @PostMapping("/import")
    @Operation(summary = "Bulk-update products (price/stock/etc.) matched by SKU")
    public ApiResponse<BulkUpdateResult> importCsv(@RequestBody List<ProductBulkRow> rows) {
        BulkUpdateResult result = productService.bulkUpdateBySku(rows);
        return ApiResponse.success("Updated " + result.updated() + " products.", result);
    }
}
