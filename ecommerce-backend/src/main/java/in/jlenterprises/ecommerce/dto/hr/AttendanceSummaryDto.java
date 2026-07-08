package in.jlenterprises.ecommerce.dto.hr;

import java.math.BigDecimal;

public record AttendanceSummaryDto(
        int year,
        int month,
        int present,
        int halfDay,
        int absent,
        int paidLeave,
        int unpaidLeave,
        int weekOff,
        int holiday,
        int totalServiceUnits,
        BigDecimal payableDays,
        int leaveRemaining
) {}
