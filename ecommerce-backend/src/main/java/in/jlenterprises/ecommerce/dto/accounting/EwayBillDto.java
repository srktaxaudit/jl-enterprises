package in.jlenterprises.ecommerce.dto.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** E-way bill working data derived from a document (for manual generation on the NIC portal). */
public record EwayBillDto(
        String documentNumber,
        LocalDate documentDate,
        String supplyType,     // OUTWARD / INWARD
        String fromGstin,
        String fromName,
        String fromAddress,
        String toGstin,
        String toName,
        String toAddress,
        BigDecimal taxableValue,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst,
        BigDecimal totalValue,
        boolean eligible,       // value >= 50,000
        String note,
        List<Item> items
) {
    public record Item(String description, String hsn, BigDecimal quantity, BigDecimal taxableValue, BigDecimal gstRate) {}
}
