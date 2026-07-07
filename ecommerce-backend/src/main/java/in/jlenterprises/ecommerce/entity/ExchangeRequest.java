package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.ExchangeStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A customer trade-in / exchange request for an old appliance against a new purchase. */
@Entity
@Table(name = "exchange_requests")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class ExchangeRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_exchange_user"))
    private User user;

    // ── Old appliance details ──
    @Column(name = "appliance_category", nullable = false, length = 80)
    private String applianceCategory;

    @Column(name = "brand", length = 80)
    private String brand;

    @Column(name = "model_number", length = 120)
    private String modelNumber;

    @Column(name = "purchase_year")
    private Integer purchaseYear;

    /** EXCELLENT / GOOD / FAIR / POOR / NOT_WORKING */
    @Column(name = "condition_grade", length = 20)
    private String conditionGrade;

    @Column(name = "working", nullable = false)
    private boolean working = true;

    @Column(name = "reason", length = 500)
    private String reason;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "exchange_request_images",
            joinColumns = @JoinColumn(name = "exchange_request_id",
                    foreignKey = @jakarta.persistence.ForeignKey(name = "fk_exchange_image")))
    @Column(name = "url", length = 500)
    private List<String> imageUrls = new ArrayList<>();

    // ── Valuation ──
    /** Customer's own expected value (optional). */
    @Column(name = "expected_value", precision = 12, scale = 2)
    private BigDecimal expectedValue;

    /** System auto-estimate at submission time. */
    @Column(name = "estimated_value", precision = 12, scale = 2)
    private BigDecimal estimatedValue = BigDecimal.ZERO;

    /** Admin-approved final value used at checkout (null until set). */
    @Column(name = "final_value", precision = 12, scale = 2)
    private BigDecimal finalValue;

    // ── New product the customer intends to buy (optional link) ──
    @Column(name = "desired_product_id")
    private UUID desiredProductId;

    @Column(name = "desired_product_name", length = 200)
    private String desiredProductName;

    // ── Workflow ──
    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_status", nullable = false, length = 25)
    private ExchangeStatus exchangeStatus = ExchangeStatus.PENDING;

    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    /** The order this exchange value was applied to (once COMPLETED). */
    @Column(name = "applied_order_id")
    private UUID appliedOrderId;

    public void addImage(String url) {
        imageUrls.add(url);
    }
}
