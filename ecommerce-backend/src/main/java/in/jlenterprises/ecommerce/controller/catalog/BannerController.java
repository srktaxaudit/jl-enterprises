package in.jlenterprises.ecommerce.controller.catalog;

import in.jlenterprises.ecommerce.dto.catalog.BannerDto;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public: active storefront banners (whitelisted for anonymous GET in SecurityConfig). */
@RestController
@RequestMapping("/api/v1/banners")
@Tag(name = "Banners", description = "Storefront promotional banners")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    @Operation(summary = "List active banners (optionally by position, e.g. HERO)")
    public ApiResponse<List<BannerDto>> list(@RequestParam(required = false) String position) {
        return ApiResponse.success(bannerService.listActive(position));
    }
}
