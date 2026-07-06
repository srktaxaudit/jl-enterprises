package in.jlenterprises.ecommerce.dto.accounting;

import in.jlenterprises.ecommerce.constant.DocumentStatus;
import in.jlenterprises.ecommerce.constant.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A row in a sales/purchase register or document list. */
public record DocumentSummaryDto(
        UUID id,
        DocumentType documentType,
        String documentNumber,
        LocalDate documentDate,
        DocumentStatus status,
        String partyName,
        BigDecimal taxableTotal,
        BigDecimal gstTotal,
        BigDecimal grandTotal
) {}
