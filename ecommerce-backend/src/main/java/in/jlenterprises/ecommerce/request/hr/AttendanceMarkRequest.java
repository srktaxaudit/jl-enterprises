package in.jlenterprises.ecommerce.request.hr;

import in.jlenterprises.ecommerce.constant.hr.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceMarkRequest(
        @NotNull UUID employeeId,
        @NotNull LocalDate workDate,
        @NotNull AttendanceStatus attendanceStatus,
        @PositiveOrZero BigDecimal hoursWorked,
        @PositiveOrZero Integer serviceUnits,
        @Size(max = 300) String note
) {}
