package in.jlenterprises.ecommerce.request.hr;

import in.jlenterprises.ecommerce.constant.hr.SalaryComponentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SalaryComponentRequest(
        @NotNull SalaryComponentType componentType,
        @NotBlank @Size(max = 80) String label,
        @NotNull @PositiveOrZero BigDecimal amount
) {}
