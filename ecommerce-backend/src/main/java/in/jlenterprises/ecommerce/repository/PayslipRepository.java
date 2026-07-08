package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.hr.Payslip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, UUID> {

    List<Payslip> findByEmployee_IdOrderByPeriodYearDescPeriodMonthDesc(UUID employeeId);

    Optional<Payslip> findByEmployee_IdAndPeriodYearAndPeriodMonth(UUID employeeId, int periodYear, int periodMonth);

    Page<Payslip> findAllByOrderByGeneratedAtDesc(Pageable pageable);

    @Query("select coalesce(sum(p.netPay), 0) from Payslip p where p.periodYear = :year and p.periodMonth = :month")
    BigDecimal sumNetPayForMonth(@Param("year") int year, @Param("month") int month);
}
