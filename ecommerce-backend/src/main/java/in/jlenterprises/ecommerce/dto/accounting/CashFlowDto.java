package in.jlenterprises.ecommerce.dto.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Cash &amp; bank movement statement for a period (direct method). */
public record CashFlowDto(
        LocalDate from,
        LocalDate to,
        BigDecimal openingBalance,
        List<Row> inflows,
        List<Row> outflows,
        BigDecimal totalInflows,
        BigDecimal totalOutflows,
        BigDecimal closingBalance
) {
    public record Row(LocalDate date, String voucherNumber, String particulars, BigDecimal amount) {}
}
