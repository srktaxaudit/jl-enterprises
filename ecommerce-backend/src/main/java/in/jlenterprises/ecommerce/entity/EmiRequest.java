package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/** A customer's request for EMI on a product, submitted from the product page. */
@Entity
@Table(name = "emi_requests")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class EmiRequest extends BaseEntity {

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /** The product they want EMI on (id kept loosely — product may later change). */
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    /** Preferred number of months (optional). */
    @Column(name = "months")
    private Integer months;

    @Column(name = "note", length = 500)
    private String note;

    /** Workflow status (own column — BaseEntity owns a generic `status`). NEW → CONTACTED → CLOSED. */
    @Column(name = "emi_status", nullable = false, length = 20)
    private String emiStatus = "NEW";
}
