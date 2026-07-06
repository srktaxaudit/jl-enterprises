package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.dto.admin.BrandingDto;
import in.jlenterprises.ecommerce.entity.AppSetting;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.repository.AppSettingRepository;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.service.SettingService;
import in.jlenterprises.ecommerce.storage.SupabaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Manage the site logo (stored in Supabase Storage; URL kept in Settings). */
@RestController
@Tag(name = "Branding", description = "Site logo — public read, admin upload/delete")
public class BrandingController {

    private static final String LOGO_KEY = "site_logo_url";
    private static final String NAME_KEY = "seller_name";
    private static final long MAX_BYTES = 2L * 1024 * 1024;   // 2 MB
    private static final Set<String> ALLOWED = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/svg+xml", "image/webp");
    private static final Map<String, String> EXT = Map.of(
            "image/png", "png", "image/jpeg", "jpg", "image/jpg", "jpg", "image/svg+xml", "svg", "image/webp", "webp");

    private final SupabaseStorageService storage;
    private final SettingService settingService;
    private final AppSettingRepository settings;

    public BrandingController(SupabaseStorageService storage, SettingService settingService,
                              AppSettingRepository settings) {
        this.storage = storage;
        this.settingService = settingService;
        this.settings = settings;
    }

    @GetMapping("/api/v1/branding")
    @Operation(summary = "Public branding (logo URL + site name)")
    public ApiResponse<BrandingDto> branding() {
        return ApiResponse.success(current());
    }

    @PostMapping("/api/v1/admin/branding/logo")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Auditable(action = "UPDATE_LOGO", entity = "branding")
    @Operation(summary = "Upload / replace the site logo")
    public ApiResponse<BrandingDto> uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new BusinessException("Choose an image to upload.");
        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED.contains(ct)) {
            throw new BusinessException("Unsupported format. Please use PNG, JPG, SVG or WebP.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "Logo must be under 2 MB.");
        }
        String objectPath = "branding/logo-" + UUID.randomUUID() + "." + EXT.getOrDefault(ct, "png");
        String url = storage.upload(objectPath, file.getBytes(), ct);

        String previous = value(LOGO_KEY);
        settingService.upsert(LOGO_KEY, url);
        if (previous != null && !previous.isBlank()) storage.deleteByPublicUrl(previous);   // best-effort cleanup
        return ApiResponse.success("Logo updated", current());
    }

    @DeleteMapping("/api/v1/admin/branding/logo")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Auditable(action = "DELETE_LOGO", entity = "branding")
    @Operation(summary = "Remove the site logo (reverts to the default)")
    public ApiResponse<BrandingDto> deleteLogo() {
        String previous = value(LOGO_KEY);
        settingService.delete(LOGO_KEY);
        if (previous != null && !previous.isBlank()) storage.deleteByPublicUrl(previous);
        return ApiResponse.success("Logo removed", current());
    }

    private BrandingDto current() {
        String logo = value(LOGO_KEY);
        String name = value(NAME_KEY);
        return new BrandingDto((logo == null || logo.isBlank()) ? null : logo,
                (name == null || name.isBlank()) ? "JL Enterprises" : name);
    }

    private String value(String key) {
        return settings.findById(key).map(AppSetting::getValue).orElse(null);
    }
}
