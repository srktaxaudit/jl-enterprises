package in.jlenterprises.ecommerce.dto.hr;

import in.jlenterprises.ecommerce.constant.hr.AttendanceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceDto(
        UUID id,
        LocalDate workDate,
        AttendanceStatus attendanceStatus,
        BigDecimal hoursWorked,
        int serviceUnits,
        String note
) {}
