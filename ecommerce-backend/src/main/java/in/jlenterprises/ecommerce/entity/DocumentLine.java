package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

/** One line of an {@link AccountingDocument}, carrying its own HSN + GST rate (multi-rate support). */
@Entity
@Table(name = "document_lines")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class DocumentLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_docline_doc"))
    private AccountingDocument document;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "quantity", precision = 15, scale = 3)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "rate", precision = 15, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate = BigDecimal.ZERO;

    // ── Computed on save ──
    @Column(name = "taxable_value", precision = 15, scale = 2) private BigDecimal taxableValue = BigDecimal.ZERO;
    @Column(name = "cgst", precision = 15, scale = 2) private BigDecimal cgst = BigDecimal.ZERO;
    @Column(name = "sgst", precision = 15, scale = 2) private BigDecimal sgst = BigDecimal.ZERO;
    @Column(name = "igst", precision = 15, scale = 2) private BigDecimal igst = BigDecimal.ZERO;
    @Column(name = "line_total", precision = 15, scale = 2) private BigDecimal lineTotal = BigDecimal.ZERO;
}
