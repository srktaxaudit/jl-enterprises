package in.jlenterprises.ecommerce.dto.accounting;

import in.jlenterprises.ecommerce.constant.DocumentStatus;
import in.jlenterprises.ecommerce.constant.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DocumentDto(
        UUID id,
        DocumentType documentType,
        String documentNumber,
        LocalDate documentDate,
        DocumentStatus status,
        UUID partyAccountId,
        String partyName,
        String partyGstin,
        String billingAddress,
        String shippingAddress,
        String placeOfSupply,
        boolean interState,
        String narration,
        String reference,
        UUID referenceDocumentId,
        String tdsSection,
        BigDecimal tdsRate,
        BigDecimal tdsAmount,
        BigDecimal subTotal,
        BigDecimal discountTotal,
        BigDecimal taxableTotal,
        BigDecimal cgstTotal,
        BigDecimal sgstTotal,
        BigDecimal igstTotal,
        BigDecimal gstTotal,
        BigDecimal roundOff,
        BigDecimal grandTotal,
        UUID journalEntryId,
        List<Line> lines
) {
    public record Line(
            UUID id,
            String description,
            String hsnCode,
            UUID productId,
            BigDecimal quantity,
            BigDecimal rate,
            BigDecimal discountPercent,
            BigDecimal gstRate,
            BigDecimal taxableValue,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal lineTotal
    ) {}
}
