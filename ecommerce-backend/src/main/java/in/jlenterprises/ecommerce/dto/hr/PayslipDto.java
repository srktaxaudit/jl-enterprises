package in.jlenterprises.ecommerce.dto.hr;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayslipDto(
        UUID id,
        UUID employeeId,
        String employeeName,
        String employeeCode,
        int periodYear,
        int periodMonth,
        Instant generatedAt,
        BigDecimal daysPresent,
        BigDecimal daysPayable,
        BigDecimal grossEarnings,
        BigDecimal totalDeductions,
        BigDecimal incentiveAmount,
        BigDecimal netPay,
        String breakdownJson,
        String notes
) {}
