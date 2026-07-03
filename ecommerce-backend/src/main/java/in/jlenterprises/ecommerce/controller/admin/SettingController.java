package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.dto.admin.SettingDto;
import in.jlenterprises.ecommerce.request.admin.SettingRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.service.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin — Settings", description = "Business settings key/value store")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    @Operation(summary = "List all settings")
    public ApiResponse<List<SettingDto>> list() {
        return ApiResponse.success(settingService.list());
    }

    @PutMapping("/{key}")
    @Auditable(action = "UPSERT_SETTING", entity = "setting")
    @Operation(summary = "Create or update a setting")
    public ApiResponse<SettingDto> upsert(@PathVariable String key, @Valid @RequestBody SettingRequest request) {
        return ApiResponse.success("Setting saved", settingService.upsert(key, request.value()));
    }

    @DeleteMapping("/{key}")
    @Auditable(action = "DELETE_SETTING", entity = "setting")
    @Operation(summary = "Delete a setting")
    public ApiResponse<Void> delete(@PathVariable String key) {
        settingService.delete(key);
        return ApiResponse.message("Setting deleted");
    }
}
