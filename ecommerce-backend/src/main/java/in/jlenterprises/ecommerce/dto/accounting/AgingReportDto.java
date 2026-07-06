package in.jlenterprises.ecommerce.dto.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Outstanding receivables/payables aged by number of days. */
public record AgingReportDto(
        String kind,          // RECEIVABLE or PAYABLE
        LocalDate asOf,
        List<PartyRow> parties,
        BigDecimal current,
        BigDecimal days31to60,
        BigDecimal days61to90,
        BigDecimal days90plus,
        BigDecimal total
) {
    public record PartyRow(
            String partyName,
            BigDecimal current,
            BigDecimal days31to60,
            BigDecimal days61to90,
            BigDecimal days90plus,
            BigDecimal total,
            int oldestDays
    ) {}
}
