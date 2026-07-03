package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.dto.cart.CartDto;
import in.jlenterprises.ecommerce.entity.Cart;
import in.jlenterprises.ecommerce.entity.CartItem;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.entity.ProductVariant;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.CartMapper;
import in.jlenterprises.ecommerce.repository.CartRepository;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import in.jlenterprises.ecommerce.repository.ProductVariantRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.request.cart.AddToCartRequest;
import in.jlenterprises.ecommerce.request.cart.UpdateCartItemRequest;
import in.jlenterprises.ecommerce.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CartMapper cartMapper;

    public CartServiceImpl(CartRepository cartRepository, UserRepository userRepository,
                           ProductRepository productRepository, ProductVariantRepository variantRepository,
                           CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.cartMapper = cartMapper;
    }

    @Override
    @Transactional
    public CartDto getCart(UUID userId) {
        return cartMapper.toDto(getOrCreate(userId));
    }

    @Override
    @Transactional
    public CartDto addItem(UUID userId, AddToCartRequest request) {
        Cart cart = getOrCreate(userId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> ResourceNotFoundException.of("Product", request.productId()));

        ProductVariant variant = null;
        if (request.variantId() != null) {
            variant = variantRepository.findById(request.variantId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Variant", request.variantId()));
        }
        UUID variantId = variant == null ? null : variant.getId();
        BigDecimal unitPrice = (variant != null && variant.getPrice() != null) ? variant.getPrice() : product.getPrice();

        // Merge with an existing identical line (matched in memory to handle null variants).
        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId())
                        && Objects.equals(i.getVariant() == null ? null : i.getVariant().getId(), variantId))
                .findFirst().orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.quantity());
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setVariant(variant);
            item.setQuantity(request.quantity());
            item.setUnitPrice(unitPrice);
            cart.getItems().add(item);
        }
        return cartMapper.toDto(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartDto updateItem(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreate(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Cart item", itemId));
        if (request.quantity() == 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(request.quantity());
        }
        return cartMapper.toDto(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartDto removeItem(UUID userId, UUID itemId) {
        Cart cart = getOrCreate(userId);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        return cartMapper.toDto(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void clear(UUID userId) {
        Cart cart = getOrCreate(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreate(UUID userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }
}
