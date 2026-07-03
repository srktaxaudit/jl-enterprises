package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * Stock record for a product. {@code available = quantity - reserved}; the order
 * service reserves on checkout and deducts on confirmation, guarded by the
 * optimistic-lock version inherited from {@link BaseEntity}.
 */
@Entity
@Table(name = "inventories", uniqueConstraints = @UniqueConstraint(name = "uk_inventory_product", columnNames = "product_id"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Inventory extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_inventory_product"))
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity = 0;

    @Column(name = "reserved", nullable = false)
    private int reserved = 0;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 3;

    @Column(name = "warehouse_location", length = 120)
    private String warehouseLocation;

    public int getAvailable() {
        return Math.max(0, quantity - reserved);
    }
}
