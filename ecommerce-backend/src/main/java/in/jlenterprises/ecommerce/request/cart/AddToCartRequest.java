package in.jlenterprises.ecommerce.request.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToCartRequest(
        @NotNull UUID productId,
        UUID variantId,
        @Min(1) @Max(99) int quantity
) {}
