package in.jlenterprises.ecommerce.dto.accounting;

import in.jlenterprises.ecommerce.constant.AccountType;
import in.jlenterprises.ecommerce.constant.DrCr;

import java.math.BigDecimal;
import java.util.UUID;

public record LedgerAccountDto(
        UUID id,
        String code,
        String name,
        AccountType accountType,
        String accountGroup,
        BigDecimal openingBalance,
        DrCr openingSide,
        BigDecimal gstRate,
        String gstin,
        String hsnCode,
        BigDecimal creditLimit,
        Integer creditDays,
        boolean blocked,
        boolean active,
        boolean systemAccount
) {}
