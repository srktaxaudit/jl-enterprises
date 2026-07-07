package in.jlenterprises.ecommerce.controller.exchange;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.ExchangeStatus;
import in.jlenterprises.ecommerce.dto.exchange.ExchangeRequestDto;
import in.jlenterprises.ecommerce.request.exchange.ExchangeCreateRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import in.jlenterprises.ecommerce.service.ExchangeService;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Exchange (trade-in) requests: customers submit + track theirs; staff manage all. */
@RestController
@Tag(name = "Exchange Requests", description = "Trade-in an old appliance toward a new purchase")
public class ExchangeController {

    private static final String STAFF = "hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER','CUSTOMER_SUPPORT')";

    private final ExchangeService service;

    public ExchangeController(ExchangeService service) {
        this.service = service;
    }

    // ── Customer (authenticated) ──
    @PostMapping("/api/v1/exchanges")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit an exchange request")
    public ResponseEntity<ApiResponse<ExchangeRequestDto>> create(@Valid @RequestBody ExchangeCreateRequest request) {
        ExchangeRequestDto dto = service.create(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Exchange request submitted", dto));
    }

    @GetMapping("/api/v1/exchanges/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my exchange requests")
    public ApiResponse<java.util.List<ExchangeRequestDto>> mine() {
        return ApiResponse.success(service.listMine(SecurityUtils.currentUserId()));
    }

    @GetMapping("/api/v1/exchanges/checkout-options")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "My approved, unused exchanges (apply at checkout)")
    public ApiResponse<java.util.List<ExchangeRequestDto>> checkoutOptions() {
        return ApiResponse.success(service.checkoutOptions(SecurityUtils.currentUserId()));
    }

    @PostMapping("/api/v1/exchanges/images")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload an image of the old appliance; returns its URL")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = service.uploadImage(SecurityUtils.currentUserId(), file);
        return ApiResponse.success("Image uploaded", Map.of("url", url));
    }

    // ── Admin / staff ──
    @GetMapping("/api/v1/admin/exchanges")
    @PreAuthorize(STAFF)
    @Operation(summary = "List all exchange requests (newest first)")
    public ApiResponse<PageResponse<ExchangeRequestDto>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(service.list(pageable)));
    }

    @GetMapping("/api/v1/admin/exchanges/{id}")
    @PreAuthorize(STAFF)
    @Operation(summary = "Get one exchange request")
    public ApiResponse<ExchangeRequestDto> get(@PathVariable UUID id) {
        return ApiResponse.success(service.get(id));
    }

    @PatchMapping("/api/v1/admin/exchanges/{id}/status")
    @PreAuthorize(STAFF)
    @Auditable(action = "UPDATE_EXCHANGE_STATUS", entity = "exchange_request")
    @Operation(summary = "Update status (+ optional internal notes)")
    public ApiResponse<ExchangeRequestDto> updateStatus(@PathVariable UUID id,
                                                        @RequestParam ExchangeStatus status,
                                                        @RequestParam(required = false) String notes) {
        return ApiResponse.success("Status updated", service.updateStatus(id, status, notes));
    }

    @PatchMapping("/api/v1/admin/exchanges/{id}/value")
    @PreAuthorize(STAFF)
    @Auditable(action = "SET_EXCHANGE_VALUE", entity = "exchange_request")
    @Operation(summary = "Set the final approved exchange value")
    public ApiResponse<ExchangeRequestDto> setValue(@PathVariable UUID id, @RequestParam BigDecimal value) {
        return ApiResponse.success("Value updated", service.setFinalValue(id, value));
    }
}
