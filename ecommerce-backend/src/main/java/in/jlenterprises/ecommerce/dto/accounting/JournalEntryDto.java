package in.jlenterprises.ecommerce.dto.accounting;

import in.jlenterprises.ecommerce.constant.VoucherType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JournalEntryDto(
        UUID id,
        String voucherNumber,
        VoucherType voucherType,
        LocalDate entryDate,
        String narration,
        String reference,
        UUID referenceId,
        List<Line> lines,
        BigDecimal totalDebit,
        BigDecimal totalCredit
) {
    public record Line(
            UUID id,
            UUID accountId,
            String accountCode,
            String accountName,
            BigDecimal debit,
            BigDecimal credit,
            String lineNarration
    ) {}
}
