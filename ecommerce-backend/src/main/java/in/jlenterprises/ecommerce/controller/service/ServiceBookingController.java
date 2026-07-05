package in.jlenterprises.ecommerce.controller.service;

import in.jlenterprises.ecommerce.dto.service.ServiceBookingDto;
import in.jlenterprises.ecommerce.request.service.ServiceBookingRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.ServiceBookingService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-bookings")
@Tag(name = "Service Bookings", description = "Storefront service requests (public create, staff manage)")
public class ServiceBookingController {

    private static final String STAFF = "hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER','CUSTOMER_SUPPORT')";

    private final ServiceBookingService service;

    public ServiceBookingController(ServiceBookingService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Submit a service request (public)")
    public ResponseEntity<ApiResponse<ServiceBookingDto>> create(@Valid @RequestBody ServiceBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service request received", service.create(request)));
    }

    @GetMapping
    @PreAuthorize(STAFF)
    @Operation(summary = "List service bookings (newest first)")
    public ApiResponse<PageResponse<ServiceBookingDto>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(service.list(pageable)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(STAFF)
    @Operation(summary = "Update a booking's status")
    public ApiResponse<ServiceBookingDto> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        return ApiResponse.success("Status updated", service.updateStatus(id, status));
    }
}
