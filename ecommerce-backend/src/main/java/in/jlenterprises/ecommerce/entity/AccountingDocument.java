package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.DocumentStatus;
import in.jlenterprises.ecommerce.constant.DocumentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A trade/accounting document (invoice, bill, estimate, credit/debit note) with GST line items. */
@Entity
@Table(name = "accounting_documents")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class AccountingDocument extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "document_number", length = 40)
    private String documentNumber;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus documentStatus = DocumentStatus.DRAFT;

    /** The party ledger (a Sundry Debtor for sales, a Sundry Creditor for purchases). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_account_id",
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_doc_party"))
    private LedgerAccount party;

    @Column(name = "party_name", length = 160)
    private String partyName;

    @Column(name = "party_gstin", length = 20)
    private String partyGstin;

    @Column(name = "billing_address", length = 500)
    private String billingAddress;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(name = "place_of_supply", length = 80)
    private String placeOfSupply;

    /** Inter-state supply → IGST; otherwise CGST + SGST. */
    @Column(name = "inter_state", nullable = false)
    private boolean interState = false;

    @Column(name = "narration", length = 500)
    private String narration;

    /** Free reference (e.g. original invoice no. for a credit note). */
    @Column(name = "reference", length = 120)
    private String reference;

    /** Link to a source document (estimate → invoice, or invoice → credit note). */
    @Column(name = "reference_document_id")
    private UUID referenceDocumentId;

    // ── TDS (typically on purchases) ──
    @Column(name = "tds_section", length = 20)
    private String tdsSection;

    @Column(name = "tds_rate", precision = 5, scale = 2)
    private BigDecimal tdsRate;

    @Column(name = "tds_amount", precision = 15, scale = 2)
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    // ── Totals ──
    @Column(name = "sub_total", precision = 15, scale = 2) private BigDecimal subTotal = BigDecimal.ZERO;
    @Column(name = "discount_total", precision = 15, scale = 2) private BigDecimal discountTotal = BigDecimal.ZERO;
    @Column(name = "taxable_total", precision = 15, scale = 2) private BigDecimal taxableTotal = BigDecimal.ZERO;
    @Column(name = "cgst_total", precision = 15, scale = 2) private BigDecimal cgstTotal = BigDecimal.ZERO;
    @Column(name = "sgst_total", precision = 15, scale = 2) private BigDecimal sgstTotal = BigDecimal.ZERO;
    @Column(name = "igst_total", precision = 15, scale = 2) private BigDecimal igstTotal = BigDecimal.ZERO;
    @Column(name = "gst_total", precision = 15, scale = 2) private BigDecimal gstTotal = BigDecimal.ZERO;
    @Column(name = "round_off", precision = 15, scale = 2) private BigDecimal roundOff = BigDecimal.ZERO;
    @Column(name = "grand_total", precision = 15, scale = 2) private BigDecimal grandTotal = BigDecimal.ZERO;

    /** The journal entry created when this document was posted (null while DRAFT). */
    @Column(name = "journal_entry_id")
    private UUID journalEntryId;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DocumentLine> lines = new ArrayList<>();

    public void addLine(DocumentLine line) {
        line.setDocument(this);
        lines.add(line);
    }
}
