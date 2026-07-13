package in.jlenterprises.ecommerce.util;

import java.time.LocalDate;

/** Indian financial year (April–March) formatting, e.g. 2026-04-01 → "2026-27". */
public final class FinancialYear {

    private FinancialYear() {}

    /** The FY label for a date: "YYYY-YY". April onwards belongs to the year that starts that April. */
    public static String of(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        int startYear = d.getMonthValue() >= 4 ? d.getYear() : d.getYear() - 1;
        return startYear + "-" + String.format("%02d", (startYear + 1) % 100);
    }
}
