package in.jlenterprises.ecommerce.controller.stock;

import in.jlenterprises.ecommerce.dto.stock.StockAlertDto;
import in.jlenterprises.ecommerce.request.stock.StockAlertCreate;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.StockAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Back-in-stock alerts: public submit from a product page + staff manage. */
@RestController
@Tag(name = "Stock Alerts", description = "Notify-me-when-in-stock requests (public create, staff manage)")
public class StockAlertController {

    private static final String STAFF = "hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER','CUSTOMER_SUPPORT')";

    private final StockAlertService service;

    public StockAlertController(StockAlertService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/stock-alerts")
    @Operation(summary = "Request a back-in-stock alert (public)")
    public ResponseEntity<ApiResponse<StockAlertDto>> create(@Valid @RequestBody StockAlertCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("We'll let you know when it's back in stock.", service.create(request)));
    }

    @GetMapping("/api/v1/admin/stock-alerts")
    @PreAuthorize(STAFF)
    @Operation(summary = "List back-in-stock requests (newest first), optionally by status")
    public ApiResponse<PageResponse<StockAlertDto>> list(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(service.list(status, pageable)));
    }

    @PatchMapping("/api/v1/admin/stock-alerts/{id}/status")
    @PreAuthorize(STAFF)
    @Operation(summary = "Update a stock alert's status (NEW/NOTIFIED/CLOSED)")
    public ApiResponse<StockAlertDto> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        return ApiResponse.success("Status updated", service.updateStatus(id, status));
    }
}
