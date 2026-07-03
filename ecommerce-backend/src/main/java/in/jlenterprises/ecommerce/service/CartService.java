package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.cart.CartDto;
import in.jlenterprises.ecommerce.request.cart.AddToCartRequest;
import in.jlenterprises.ecommerce.request.cart.UpdateCartItemRequest;

import java.util.UUID;

public interface CartService {

    CartDto getCart(UUID userId);

    CartDto addItem(UUID userId, AddToCartRequest request);

    CartDto updateItem(UUID userId, UUID itemId, UpdateCartItemRequest request);

    CartDto removeItem(UUID userId, UUID itemId);

    void clear(UUID userId);
}
