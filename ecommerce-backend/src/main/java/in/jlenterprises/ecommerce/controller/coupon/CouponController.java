package in.jlenterprises.ecommerce.controller.coupon;

import in.jlenterprises.ecommerce.dto.coupon.CouponDto;
import in.jlenterprises.ecommerce.dto.coupon.CouponValidationResult;
import in.jlenterprises.ecommerce.request.coupon.CouponRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import in.jlenterprises.ecommerce.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "Coupons", description = "Coupon management (admin) and validation (customer)")
public class CouponController {

    private static final String STAFF = "hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER')";

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    @PreAuthorize(STAFF)
    @Operation(summary = "List all coupons (staff)")
    public ApiResponse<List<CouponDto>> list() {
        return ApiResponse.success(couponService.list());
    }

    @GetMapping("/active")
    @Operation(summary = "List active coupons customers can use (public)")
    public ApiResponse<List<CouponDto>> active() {
        return ApiResponse.success(couponService.activePublic());
    }

    @GetMapping("/eligible")
    @Operation(summary = "Coupons the current customer can apply to this subtotal (authenticated)")
    public ApiResponse<List<CouponDto>> eligible(@RequestParam(required = false) BigDecimal subtotal) {
        return ApiResponse.success(couponService.eligibleFor(subtotal, SecurityUtils.currentUserId()));
    }

    @PostMapping
    @PreAuthorize(STAFF)
    @Operation(summary = "Create a coupon")
    public ResponseEntity<ApiResponse<CouponDto>> create(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupon created", couponService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(STAFF)
    @Operation(summary = "Update a coupon")
    public ApiResponse<CouponDto> update(@PathVariable UUID id, @Valid @RequestBody CouponRequest request) {
        return ApiResponse.success("Coupon updated", couponService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(STAFF)
    @Operation(summary = "Delete a coupon")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        couponService.delete(id);
        return ApiResponse.message("Coupon deleted");
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate a coupon against a subtotal for the current user")
    public ApiResponse<CouponValidationResult> validate(@RequestParam String code, @RequestParam BigDecimal subtotal) {
        return ApiResponse.success(couponService.validate(code, subtotal, SecurityUtils.currentUserId()));
    }
}
