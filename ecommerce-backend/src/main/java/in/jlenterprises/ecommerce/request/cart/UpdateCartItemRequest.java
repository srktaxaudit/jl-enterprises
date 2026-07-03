package in.jlenterprises.ecommerce.request.cart;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(0) int quantity   // 0 removes the line
) {}
