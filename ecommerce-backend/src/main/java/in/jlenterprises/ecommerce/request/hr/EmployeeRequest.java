package in.jlenterprises.ecommerce.request.hr;

import in.jlenterprises.ecommerce.constant.hr.EmploymentStatus;
import in.jlenterprises.ecommerce.constant.hr.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank @Size(max = 40) String employeeCode,
        @NotBlank @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Size(max = 20) String phone,
        @Size(max = 160) String email,
        @Size(max = 500) String photoUrl,
        @Size(max = 120) String designation,
        @Size(max = 120) String department,
        LocalDate dateOfJoining,
        @NotNull EmploymentType employmentType,
        EmploymentStatus employmentStatus,
        @Size(max = 120) String bankAccountName,
        @Size(max = 40) String bankAccountNumber,
        @Size(max = 20) String ifsc,
        @Size(max = 20) String pan,
        @Size(max = 300) String address,
        @PositiveOrZero BigDecimal monthlyBasic,
        @PositiveOrZero BigDecimal dailyRate,
        @PositiveOrZero BigDecimal perServiceRate,
        @PositiveOrZero BigDecimal hourlyRate,
        @PositiveOrZero Integer annualPaidLeave
) {}
