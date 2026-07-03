package in.jlenterprises.ecommerce.controller.customer;

import in.jlenterprises.ecommerce.dto.customer.WishlistDto;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import in.jlenterprises.ecommerce.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@Tag(name = "Wishlist", description = "The current user's wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    @Operation(summary = "Get my wishlist")
    public ApiResponse<WishlistDto> get() {
        return ApiResponse.success(wishlistService.get(SecurityUtils.currentUserId()));
    }

    @PostMapping("/items/{productId}")
    @Operation(summary = "Add a product to the wishlist")
    public ApiResponse<WishlistDto> add(@PathVariable UUID productId) {
        return ApiResponse.success("Added to wishlist", wishlistService.add(SecurityUtils.currentUserId(), productId));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove a product from the wishlist")
    public ApiResponse<WishlistDto> remove(@PathVariable UUID productId) {
        return ApiResponse.success("Removed from wishlist", wishlistService.remove(SecurityUtils.currentUserId(), productId));
    }
}
