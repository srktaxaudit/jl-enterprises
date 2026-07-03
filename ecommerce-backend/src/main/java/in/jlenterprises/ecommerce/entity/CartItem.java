package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/** A line in a cart: a product (optionally a specific variant) and a quantity. */
@Entity
@Table(name = "cart_items", indexes = @Index(name = "idx_cartitem_cart", columnList = "cart_id"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_cartitem_cart"))
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_cartitem_product"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_cartitem_variant"))
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    /** Unit price captured when the item was added (for display / drift detection). */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
}
