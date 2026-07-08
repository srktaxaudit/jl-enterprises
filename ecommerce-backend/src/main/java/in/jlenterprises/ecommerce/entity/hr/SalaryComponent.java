package in.jlenterprises.ecommerce.entity.hr;

import in.jlenterprises.ecommerce.constant.hr.SalaryComponentType;
import in.jlenterprises.ecommerce.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/** A recurring monthly salary line (earning or deduction) for an employee. */
@Entity
@Table(name = "hr_salary_components")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class SalaryComponent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_hr_comp_employee"))
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 20)
    private SalaryComponentType componentType;

    @Column(name = "label", nullable = false, length = 80)
    private String label;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
}
