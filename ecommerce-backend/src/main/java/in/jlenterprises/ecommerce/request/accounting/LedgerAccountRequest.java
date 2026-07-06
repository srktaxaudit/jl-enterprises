package in.jlenterprises.ecommerce.request.accounting;

import in.jlenterprises.ecommerce.constant.AccountType;
import in.jlenterprises.ecommerce.constant.DrCr;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LedgerAccountRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 160) String name,
        @NotNull AccountType accountType,
        @Size(max = 80) String accountGroup,
        @PositiveOrZero BigDecimal openingBalance,
        DrCr openingSide,
        @PositiveOrZero BigDecimal gstRate,
        @Size(max = 20) String gstin,
        @Size(max = 20) String hsnCode,
        @PositiveOrZero BigDecimal creditLimit,
        @PositiveOrZero Integer creditDays,
        Boolean blocked,
        Boolean active
) {}
