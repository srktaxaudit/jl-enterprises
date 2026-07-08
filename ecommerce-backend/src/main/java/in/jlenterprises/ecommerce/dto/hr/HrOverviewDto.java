package in.jlenterprises.ecommerce.dto.hr;

import java.math.BigDecimal;

public record HrOverviewDto(
        long totalEmployees,
        long activeEmployees,
        long onLeave,
        long presentToday,
        int year,
        int month,
        BigDecimal monthPayrollTotal
) {}
