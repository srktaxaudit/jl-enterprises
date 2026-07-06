package in.jlenterprises.ecommerce.dto.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Generic grouped statement used for both Profit &amp; Loss and Balance Sheet. */
public record FinancialStatementDto(
        String title,
        LocalDate from,
        LocalDate to,
        List<Section> sections,
        String resultLabel,
        BigDecimal result
) {
    public record Section(String name, List<Line> lines, BigDecimal total) {}

    public record Line(String code, String name, BigDecimal amount) {}
}
