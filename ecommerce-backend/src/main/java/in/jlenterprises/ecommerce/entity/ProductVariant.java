package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A specific purchasable variant of a product (e.g. "1.5 Ton / White"). Holds
 * its own SKU, price override and option values.
 */
@Entity
@Table(name = "product_variants",
        uniqueConstraints = @UniqueConstraint(name = "uk_variant_sku", columnNames = "sku"),
        indexes = @Index(name = "idx_variant_product", columnList = "product_id"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_variant_product"))
    private Product product;

    @Column(name = "sku", nullable = false, length = 80)
    private String sku;

    @Column(name = "title", length = 160)
    private String title;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_price", precision = 12, scale = 2)
    private BigDecimal comparePrice;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VariantOption> options = new ArrayList<>();
}
