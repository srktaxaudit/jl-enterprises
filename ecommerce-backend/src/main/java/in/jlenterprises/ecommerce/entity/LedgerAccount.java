package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.AccountType;
import in.jlenterprises.ecommerce.constant.DrCr;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/** A ledger account in the chart of accounts (double-entry). */
@Entity
@Table(name = "ledger_accounts", uniqueConstraints = @UniqueConstraint(name = "uk_ledger_code", columnNames = "code"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class LedgerAccount extends BaseEntity {

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /** Free-text grouping, e.g. "Sundry Debtors", "Bank Accounts", "Duties & Taxes". */
    @Column(name = "account_group", length = 80)
    private String accountGroup;

    @Column(name = "opening_balance", precision = 15, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "opening_side", length = 2)
    private DrCr openingSide = DrCr.DR;

    /** Default GST rate % for this account/party (multi-rate support). */
    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "gstin", length = 20)
    private String gstin;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    /** Credit control: max outstanding value and/or days allowed. */
    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "credit_days")
    private Integer creditDays;

    /** Blocked = no new transactions may post; inactive = hidden from pickers. */
    @Column(name = "blocked", nullable = false)
    private boolean blocked = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Seeded/system account (e.g. Sales, Output GST) — protected from deletion. */
    @Column(name = "system_account", nullable = false)
    private boolean systemAccount = false;
}
