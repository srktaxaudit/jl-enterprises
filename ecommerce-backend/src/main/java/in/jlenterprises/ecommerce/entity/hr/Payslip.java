package in.jlenterprises.ecommerce.entity.hr;

import in.jlenterprises.ecommerce.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;

/** A generated monthly salary slip — computed totals + an immutable line-item snapshot. */
@Entity
@Table(name = "hr_payslips",
        indexes = @Index(name = "idx_hr_payslip_emp", columnList = "employee_id"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Payslip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_hr_payslip_employee"))
    private Employee employee;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "days_present", precision = 6, scale = 2)
    private BigDecimal daysPresent = BigDecimal.ZERO;

    @Column(name = "days_payable", precision = 6, scale = 2)
    private BigDecimal daysPayable = BigDecimal.ZERO;

    @Column(name = "gross_earnings", precision = 12, scale = 2)
    private BigDecimal grossEarnings = BigDecimal.ZERO;

    @Column(name = "total_deductions", precision = 12, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "incentive_amount", precision = 12, scale = 2)
    private BigDecimal incentiveAmount = BigDecimal.ZERO;

    @Column(name = "net_pay", precision = 12, scale = 2)
    private BigDecimal netPay = BigDecimal.ZERO;

    /** JSON snapshot of the earning/deduction lines used, so the slip never changes. */
    @Column(name = "breakdown_json", length = 4000)
    private String breakdownJson;

    @Column(name = "notes", length = 500)
    private String notes;
}
