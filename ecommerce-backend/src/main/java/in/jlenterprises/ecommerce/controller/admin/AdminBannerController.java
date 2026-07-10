package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.dto.catalog.BannerDto;
import in.jlenterprises.ecommerce.request.admin.BannerRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/banners")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MARKETING_MANAGER')")
@Tag(name = "Admin — Banners", description = "Manage storefront banners")
public class AdminBannerController {

    private final BannerService bannerService;

    public AdminBannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    @Operation(summary = "List all banners (incl. inactive)")
    public ApiResponse<List<BannerDto>> list() {
        return ApiResponse.success(bannerService.listAll());
    }

    @PostMapping
    @Auditable(action = "CREATE_BANNER", entity = "banner")
    @Operation(summary = "Create a banner")
    public ApiResponse<BannerDto> create(@Valid @RequestBody BannerRequest request) {
        return ApiResponse.success("Banner created", bannerService.create(request));
    }

    @PutMapping("/{id}")
    @Auditable(action = "UPDATE_BANNER", entity = "banner")
    @Operation(summary = "Update a banner")
    public ApiResponse<BannerDto> update(@PathVariable UUID id, @Valid @RequestBody BannerRequest request) {
        return ApiResponse.success("Banner updated", bannerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Auditable(action = "DELETE_BANNER", entity = "banner")
    @Operation(summary = "Delete a banner")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        bannerService.delete(id);
        return ApiResponse.message("Banner deleted");
    }

    @PostMapping("/image")
    @Operation(summary = "Upload a banner image; returns its public URL")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(Map.of("url", bannerService.uploadImage(file)));
    }
}
