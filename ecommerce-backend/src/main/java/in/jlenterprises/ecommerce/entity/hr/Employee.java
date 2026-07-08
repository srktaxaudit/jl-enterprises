package in.jlenterprises.ecommerce.entity.hr;

import in.jlenterprises.ecommerce.constant.hr.EmploymentStatus;
import in.jlenterprises.ecommerce.constant.hr.EmploymentType;
import in.jlenterprises.ecommerce.entity.BaseEntity;
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
import java.time.LocalDate;
import java.util.UUID;

/** An HR-managed employee record (no login). Pay basis fields are used per employmentType. */
@Entity
@Table(name = "hr_employees",
        uniqueConstraints = @UniqueConstraint(name = "uk_hr_employee_code", columnNames = "employee_code"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Employee extends BaseEntity {

    @Column(name = "employee_code", nullable = false, length = 40)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "designation", length = 120)
    private String designation;

    @Column(name = "department", length = 120)
    private String department;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Column(name = "bank_account_name", length = 120)
    private String bankAccountName;

    @Column(name = "bank_account_number", length = 40)
    private String bankAccountNumber;

    @Column(name = "ifsc", length = 20)
    private String ifsc;

    @Column(name = "pan", length = 20)
    private String pan;

    @Column(name = "address", length = 300)
    private String address;

    // ── Pay basis ──
    @Column(name = "monthly_basic", precision = 12, scale = 2)
    private BigDecimal monthlyBasic = BigDecimal.ZERO;

    @Column(name = "daily_rate", precision = 12, scale = 2)
    private BigDecimal dailyRate = BigDecimal.ZERO;

    @Column(name = "per_service_rate", precision = 12, scale = 2)
    private BigDecimal perServiceRate = BigDecimal.ZERO;

    @Column(name = "hourly_rate", precision = 12, scale = 2)
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(name = "annual_paid_leave", nullable = false)
    private int annualPaidLeave = 12;

    /** Optional link to a staff User (reserved for future self-service). */
    @Column(name = "user_id")
    private UUID userId;
}
