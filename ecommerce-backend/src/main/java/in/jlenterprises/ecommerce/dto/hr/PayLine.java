package in.jlenterprises.ecommerce.dto.hr;

import java.math.BigDecimal;

/** One line of a payslip breakdown (serialized into Payslip.breakdownJson). */
public record PayLine(String label, String type, BigDecimal amount) {}
