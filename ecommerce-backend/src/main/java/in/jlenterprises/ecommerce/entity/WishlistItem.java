package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** A product saved in a wishlist. Unique per (wishlist, product). */
@Entity
@Table(name = "wishlist_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_wishlist_product", columnNames = {"wishlist_id", "product_id"}),
        indexes = @Index(name = "idx_wishlistitem_wishlist", columnList = "wishlist_id"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class WishlistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wishlist_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_wishlistitem_wishlist"))
    private Wishlist wishlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_wishlistitem_product"))
    private Product product;
}
