package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.dto.customer.WishlistDto;
import in.jlenterprises.ecommerce.dto.customer.WishlistItemDto;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.entity.Wishlist;
import in.jlenterprises.ecommerce.entity.WishlistItem;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.ProductMapper;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.repository.WishlistRepository;
import in.jlenterprises.ecommerce.service.WishlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public WishlistServiceImpl(WishlistRepository wishlistRepository, UserRepository userRepository,
                               ProductRepository productRepository, ProductMapper productMapper) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional
    public WishlistDto get(UUID userId) {
        return toDto(getOrCreate(userId));
    }

    @Override
    @Transactional
    public WishlistDto add(UUID userId, UUID productId) {
        Wishlist wishlist = getOrCreate(userId);
        boolean already = wishlist.getItems().stream()
                .anyMatch(i -> i.getProduct().getId().equals(productId));
        if (!already) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
            WishlistItem item = new WishlistItem();
            item.setWishlist(wishlist);
            item.setProduct(product);
            wishlist.getItems().add(item);
            wishlist = wishlistRepository.save(wishlist);
        }
        return toDto(wishlist);
    }

    @Override
    @Transactional
    public WishlistDto remove(UUID userId, UUID productId) {
        Wishlist wishlist = getOrCreate(userId);
        wishlist.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        return toDto(wishlistRepository.save(wishlist));
    }

    private WishlistDto toDto(Wishlist wishlist) {
        List<WishlistItemDto> items = wishlist.getItems().stream()
                .map(i -> new WishlistItemDto(i.getId(), productMapper.toSummary(i.getProduct())))
                .toList();
        return new WishlistDto(wishlist.getId(), items);
    }

    private Wishlist getOrCreate(UUID userId) {
        return wishlistRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(user);
            return wishlistRepository.save(wishlist);
        });
    }
}
