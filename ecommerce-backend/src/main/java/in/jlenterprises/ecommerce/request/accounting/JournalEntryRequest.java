package in.jlenterprises.ecommerce.request.accounting;

import in.jlenterprises.ecommerce.constant.VoucherType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JournalEntryRequest(
        VoucherType voucherType,
        @NotNull LocalDate entryDate,
        @Size(max = 500) String narration,
        @Size(max = 120) String reference,
        @NotEmpty @Valid List<Line> lines
) {
    public record Line(
            @NotNull UUID accountId,
            @PositiveOrZero BigDecimal debit,
            @PositiveOrZero BigDecimal credit,
            @Size(max = 300) String lineNarration
    ) {}
}
