package in.jlenterprises.ecommerce.request.hr;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PayslipGenerateRequest(
        @NotNull UUID employeeId,
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull @Min(1) @Max(12) Integer month,
        @PositiveOrZero BigDecimal incentiveAmount,
        @Size(max = 500) String notes
) {}
