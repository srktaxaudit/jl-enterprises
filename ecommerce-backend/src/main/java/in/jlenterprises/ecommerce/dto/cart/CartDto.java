package in.jlenterprises.ecommerce.dto.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartDto(
        UUID id,
        List<CartItemDto> items,
        int itemCount,
        BigDecimal subtotal
) {}
