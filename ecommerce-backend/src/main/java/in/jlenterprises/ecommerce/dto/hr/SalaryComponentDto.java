package in.jlenterprises.ecommerce.dto.hr;

import in.jlenterprises.ecommerce.constant.hr.SalaryComponentType;

import java.math.BigDecimal;
import java.util.UUID;

public record SalaryComponentDto(UUID id, SalaryComponentType componentType, String label, BigDecimal amount) {}
