package in.jlenterprises.ecommerce.dto.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** GSTR-1 (outward) or GSTR-2 (inward) working data for a period. */
public record GstReturnDto(
        String returnType,
        LocalDate from,
        LocalDate to,
        BigDecimal taxableTotal,
        BigDecimal cgstTotal,
        BigDecimal sgstTotal,
        BigDecimal igstTotal,
        BigDecimal gstTotal,
        List<Section> sections,
        List<HsnRow> hsnSummary
) {
    public record Section(String title, List<Row> rows,
                          BigDecimal taxable, BigDecimal cgst, BigDecimal sgst, BigDecimal igst) {}

    public record Row(String documentNumber, LocalDate date, String partyName, String gstin,
                      BigDecimal taxable, BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal total) {}

    public record HsnRow(String hsn, BigDecimal gstRate, BigDecimal taxable,
                         BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal total) {}
}
