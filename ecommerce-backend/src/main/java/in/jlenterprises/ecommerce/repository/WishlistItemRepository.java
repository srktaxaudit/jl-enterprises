package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    boolean existsByWishlistIdAndProductId(UUID wishlistId, UUID productId);

    Optional<WishlistItem> findByIdAndWishlistId(UUID id, UUID wishlistId);

    void deleteByWishlistIdAndProductId(UUID wishlistId, UUID productId);
}
