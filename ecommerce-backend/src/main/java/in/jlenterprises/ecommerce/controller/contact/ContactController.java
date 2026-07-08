package in.jlenterprises.ecommerce.controller.contact;

import in.jlenterprises.ecommerce.dto.contact.ContactEnquiryDto;
import in.jlenterprises.ecommerce.request.contact.ContactRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.ContactService;
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

/** Contact enquiries: public submit + staff manage. */
@RestController
@Tag(name = "Contact Enquiries", description = "Storefront Contact Us form (public create, staff manage)")
public class ContactController {

    private static final String STAFF = "hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER','CUSTOMER_SUPPORT')";

    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/contact")
    @Operation(summary = "Submit a contact enquiry (public)")
    public ResponseEntity<ApiResponse<ContactEnquiryDto>> create(@Valid @RequestBody ContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thanks — we'll get back to you shortly.", service.create(request)));
    }

    @GetMapping("/api/v1/admin/contact-enquiries")
    @PreAuthorize(STAFF)
    @Operation(summary = "List contact enquiries (newest first), optionally by status")
    public ApiResponse<PageResponse<ContactEnquiryDto>> list(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(service.list(status, pageable)));
    }

    @PatchMapping("/api/v1/admin/contact-enquiries/{id}/status")
    @PreAuthorize(STAFF)
    @Operation(summary = "Update an enquiry's status (NEW/READ/CLOSED)")
    public ApiResponse<ContactEnquiryDto> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        return ApiResponse.success("Status updated", service.updateStatus(id, status));
    }
}
