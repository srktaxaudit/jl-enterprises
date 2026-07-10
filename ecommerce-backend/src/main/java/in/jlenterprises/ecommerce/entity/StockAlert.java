package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/** A "notify me when back in stock" request submitted from an out-of-stock product page. */
@Entity
@Table(name = "stock_alerts")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class StockAlert extends BaseEntity {

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    /** Workflow status (own column — BaseEntity owns a generic `status`). NEW → NOTIFIED → CLOSED. */
    @Column(name = "alert_status", nullable = false, length = 20)
    private String alertStatus = "NEW";
}
