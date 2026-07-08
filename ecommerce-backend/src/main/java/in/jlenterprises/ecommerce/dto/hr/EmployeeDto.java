package in.jlenterprises.ecommerce.dto.hr;

import in.jlenterprises.ecommerce.constant.hr.EmploymentStatus;
import in.jlenterprises.ecommerce.constant.hr.EmploymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EmployeeDto(
        UUID id,
        String employeeCode,
        String firstName,
        String lastName,
        String phone,
        String email,
        String photoUrl,
        String designation,
        String department,
        LocalDate dateOfJoining,
        EmploymentType employmentType,
        EmploymentStatus employmentStatus,
        String bankAccountName,
        String bankAccountNumber,
        String ifsc,
        String pan,
        String address,
        BigDecimal monthlyBasic,
        BigDecimal dailyRate,
        BigDecimal perServiceRate,
        BigDecimal hourlyRate,
        int annualPaidLeave,
        List<SalaryComponentDto> components,
        Instant createdAt
) {}
