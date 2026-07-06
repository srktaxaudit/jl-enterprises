package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.constant.RoleName;
import in.jlenterprises.ecommerce.dto.admin.RoleDto;
import in.jlenterprises.ecommerce.dto.auth.UserDto;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin — Users & Roles", description = "User and role administration (admin only)")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER')")
    @Operation(summary = "List users (paged, optional search)")
    public ApiResponse<PageResponse<UserDto>> listUsers(@RequestParam(required = false) String search,
                                                        @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(adminUserService.listUsers(search, pageable)));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER')")
    @Operation(summary = "Get a user by id")
    public ApiResponse<UserDto> getUser(@PathVariable UUID id) {
        return ApiResponse.success(adminUserService.getUser(id));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Enable or disable a user")
    public ApiResponse<UserDto> setEnabled(@PathVariable UUID id, @RequestParam boolean enabled) {
        return ApiResponse.success("User status updated", adminUserService.setEnabled(id, enabled));
    }

    @PostMapping("/users/{id}/roles/{role}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Assign a role to a user (super-admin only)")
    public ApiResponse<UserDto> assignRole(@PathVariable UUID id, @PathVariable RoleName role) {
        return ApiResponse.success("Role assigned", adminUserService.assignRole(id, role));
    }

    @DeleteMapping("/users/{id}/roles/{role}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Remove a role from a user (super-admin only)")
    public ApiResponse<UserDto> removeRole(@PathVariable UUID id, @PathVariable RoleName role) {
        return ApiResponse.success("Role removed", adminUserService.removeRole(id, role));
    }

    @GetMapping("/roles")
    @Operation(summary = "List all roles")
    public ApiResponse<List<RoleDto>> listRoles() {
        return ApiResponse.success(adminUserService.listRoles());
    }
}
