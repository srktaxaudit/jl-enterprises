package in.jlenterprises.ecommerce.dto.customer;

import java.util.List;
import java.util.UUID;

public record WishlistDto(
        UUID id,
        List<WishlistItemDto> items
) {}
