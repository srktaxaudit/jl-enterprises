package in.jlenterprises.ecommerce.controller.emi;

import in.jlenterprises.ecommerce.dto.emi.EmiRequestDto;
import in.jlenterprises.ecommerce.request.emi.EmiRequestCreate;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.EmiRequestService;
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

/** EMI requests: public submit from a product page + staff manage. */
@RestController
@Tag(name = "EMI Requests", description = "Customer EMI enquiries on products (public create, staff manage)")
public class EmiRequestController {

    private static final String STAFF = "hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER','CUSTOMER_SUPPORT')";

    private final EmiRequestService service;

    public EmiRequestController(EmiRequestService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/emi-requests")
    @Operation(summary = "Submit an EMI request (public)")
    public ResponseEntity<ApiResponse<EmiRequestDto>> create(@Valid @RequestBody EmiRequestCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("EMI request received — our team will call you.", service.create(request)));
    }

    @GetMapping("/api/v1/admin/emi-requests")
    @PreAuthorize(STAFF)
    @Operation(summary = "List EMI requests (newest first), optionally by status")
    public ApiResponse<PageResponse<EmiRequestDto>> list(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(service.list(status, pageable)));
    }

    @PatchMapping("/api/v1/admin/emi-requests/{id}/status")
    @PreAuthorize(STAFF)
    @Operation(summary = "Update an EMI request's status (NEW/CONTACTED/CLOSED)")
    public ApiResponse<EmiRequestDto> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        return ApiResponse.success("Status updated", service.updateStatus(id, status));
    }
}
