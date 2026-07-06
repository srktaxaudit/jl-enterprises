package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.VoucherType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A balanced double-entry voucher: the sum of its line debits equals its credits. */
@Entity
@Table(name = "journal_entries")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class JournalEntry extends BaseEntity {

    @Column(name = "voucher_number", length = 40)
    private String voucherNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_type", nullable = false, length = 20)
    private VoucherType voucherType = VoucherType.JOURNAL;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "narration", length = 500)
    private String narration;

    /** Human reference (e.g. an order number or bill number). */
    @Column(name = "reference", length = 120)
    private String reference;

    /** Optional link to the source record (e.g. the order id) that produced this entry. */
    @Column(name = "reference_id")
    private UUID referenceId;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JournalLine> lines = new ArrayList<>();

    public void addLine(JournalLine line) {
        line.setJournalEntry(this);
        lines.add(line);
    }
}
