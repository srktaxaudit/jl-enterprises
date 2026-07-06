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

/** One posting line of a {@link JournalEntry}: an amount to one account, on the debit OR credit side. */
@Entity
@Table(name = "journal_lines")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class JournalLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_jline_entry"))
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_jline_account"))
    private LedgerAccount account;

    @Column(name = "debit", nullable = false, precision = 15, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(name = "credit", nullable = false, precision = 15, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(name = "line_narration", length = 300)
    private String lineNarration;
}
