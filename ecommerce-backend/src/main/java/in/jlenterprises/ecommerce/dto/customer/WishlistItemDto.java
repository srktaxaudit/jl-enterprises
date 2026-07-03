package in.jlenterprises.ecommerce.dto.customer;

import in.jlenterprises.ecommerce.dto.catalog.ProductSummaryDto;

import java.util.UUID;

public record WishlistItemDto(
        UUID id,
        ProductSummaryDto product
) {}
