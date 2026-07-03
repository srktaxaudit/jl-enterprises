package in.jlenterprises.ecommerce.controller.customer;

import in.jlenterprises.ecommerce.dto.cart.CartDto;
import in.jlenterprises.ecommerce.request.cart.AddToCartRequest;
import in.jlenterprises.ecommerce.request.cart.UpdateCartItemRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import in.jlenterprises.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "The current user's shopping cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get my cart")
    public ApiResponse<CartDto> getCart() {
        return ApiResponse.success(cartService.getCart(SecurityUtils.currentUserId()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart")
    public ApiResponse<CartDto> add(@Valid @RequestBody AddToCartRequest request) {
        return ApiResponse.success("Item added", cartService.addItem(SecurityUtils.currentUserId(), request));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update a cart item's quantity (0 removes it)")
    public ApiResponse<CartDto> update(@PathVariable UUID itemId, @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.updateItem(SecurityUtils.currentUserId(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove a cart item")
    public ApiResponse<CartDto> remove(@PathVariable UUID itemId) {
        return ApiResponse.success(cartService.removeItem(SecurityUtils.currentUserId(), itemId));
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart")
    public ApiResponse<Void> clear() {
        cartService.clear(SecurityUtils.currentUserId());
        return ApiResponse.message("Cart cleared");
    }
}
