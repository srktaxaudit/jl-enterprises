package in.jlenterprises.ecommerce.dto.accounting;

import in.jlenterprises.ecommerce.constant.AccountType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrialBalanceDto(
        LocalDate asOf,
        List<Row> rows,
        BigDecimal totalDebit,
        BigDecimal totalCredit
) {
    public record Row(
            String code,
            String name,
            AccountType accountType,
            BigDecimal debit,
            BigDecimal credit
    ) {}
}
