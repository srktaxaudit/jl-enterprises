package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.customer.WishlistDto;

import java.util.UUID;

public interface WishlistService {

    WishlistDto get(UUID userId);

    WishlistDto add(UUID userId, UUID productId);

    WishlistDto remove(UUID userId, UUID productId);
}
