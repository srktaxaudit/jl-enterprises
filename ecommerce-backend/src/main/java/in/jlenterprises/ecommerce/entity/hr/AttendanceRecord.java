package in.jlenterprises.ecommerce.entity.hr;

import in.jlenterprises.ecommerce.constant.hr.AttendanceStatus;
import in.jlenterprises.ecommerce.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One employee's attendance on one day. */
@Entity
@Table(name = "hr_attendance",
        uniqueConstraints = @UniqueConstraint(name = "uk_hr_attendance_emp_date", columnNames = {"employee_id", "work_date"}),
        indexes = @Index(name = "idx_hr_attendance_emp", columnList = "employee_id"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class AttendanceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_hr_att_employee"))
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    private AttendanceStatus attendanceStatus;

    @Column(name = "hours_worked", precision = 5, scale = 2)
    private BigDecimal hoursWorked;

    /** Completed services/units on this day (for SERVICE_BASED pay). */
    @Column(name = "service_units", nullable = false)
    private int serviceUnits = 0;

    @Column(name = "note", length = 300)
    private String note;
}
