package in.jlenterprises.ecommerce.dto.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;

/** GSTR-3B summary: outward tax liability vs inward ITC, and net payable. */
public record Gstr3bDto(
        LocalDate from,
        LocalDate to,
        BigDecimal outwardTaxable,
        BigDecimal outwardCgst,
        BigDecimal outwardSgst,
        BigDecimal outwardIgst,
        BigDecimal inwardTaxable,
        BigDecimal itcCgst,
        BigDecimal itcSgst,
        BigDecimal itcIgst,
        BigDecimal netCgst,
        BigDecimal netSgst,
        BigDecimal netIgst,
        BigDecimal netPayable
) {}
