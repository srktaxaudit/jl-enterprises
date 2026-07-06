package in.jlenterprises.ecommerce.dto.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LedgerStatementDto(
        UUID accountId,
        String accountCode,
        String accountName,
        LocalDate from,
        LocalDate to,
        BigDecimal openingBalance,   // signed: +ve = debit balance, -ve = credit balance
        List<Line> lines,
        BigDecimal closingBalance
) {
    public record Line(
            LocalDate date,
            String voucherNumber,
            String narration,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal runningBalance
    ) {}
}
