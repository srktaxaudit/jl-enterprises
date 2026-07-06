package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.dto.auth.UserDto;
import in.jlenterprises.ecommerce.request.admin.PasswordResetRequest;
import in.jlenterprises.ecommerce.request.admin.StaffRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.AdminUserService;
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

import java.util.UUID;

/** Staff account management — admin/super-admin only. */
@RestController
@RequestMapping("/api/v1/admin/staff")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin — Staff", description = "Create and manage staff accounts, roles and access")
public class AdminStaffController {

    private final AdminUserService adminUserService;

    public AdminStaffController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "List staff accounts (paged, optional search)")
    public ApiResponse<PageResponse<UserDto>> list(@RequestParam(required = false) String search,
                                                   @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(adminUserService.listStaff(search, pageable)));
    }

    @PostMapping
    @Operation(summary = "Create a staff account")
    public ResponseEntity<ApiResponse<UserDto>> create(@Valid @RequestBody StaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff account created", adminUserService.createStaff(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a staff account (details + roles)")
    public ApiResponse<UserDto> update(@PathVariable UUID id, @Valid @RequestBody StaffRequest request) {
        return ApiResponse.success("Staff updated", adminUserService.updateStaff(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a staff account")
    public ApiResponse<UserDto> setEnabled(@PathVariable UUID id, @RequestParam boolean enabled) {
        return ApiResponse.success("Status updated", adminUserService.setEnabled(id, enabled));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset a staff member's password")
    public ApiResponse<UserDto> resetPassword(@PathVariable UUID id, @Valid @RequestBody PasswordResetRequest request) {
        return ApiResponse.success("Password reset", adminUserService.resetPassword(id, request.password()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a staff account")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        adminUserService.deleteStaff(id);
        return ApiResponse.message("Staff account removed");
    }
}
