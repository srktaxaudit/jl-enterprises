package in.jlenterprises.ecommerce.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * GST helpers for GST-INCLUSIVE pricing (Indian retail convention — the price the
 * customer pays already includes tax). These back-calculate the tax component from
 * an inclusive amount, so the amount charged never changes — only the invoice/report
 * breakup is derived.
 */
public final class GstUtil {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private GstUtil() {}

    /** Pre-tax (taxable) value embedded in a GST-inclusive amount at the given rate %. */
    public static BigDecimal taxableValue(BigDecimal inclusiveAmount, BigDecimal ratePercent) {
        if (inclusiveAmount == null) return BigDecimal.ZERO;
        BigDecimal amt = inclusiveAmount.setScale(2, RoundingMode.HALF_UP);
        if (ratePercent == null || ratePercent.signum() <= 0) return amt;
        return amt.multiply(HUNDRED).divide(HUNDRED.add(ratePercent), 2, RoundingMode.HALF_UP);
    }

    /** GST amount embedded in a GST-inclusive amount at the given rate %. */
    public static BigDecimal gstAmount(BigDecimal inclusiveAmount, BigDecimal ratePercent) {
        if (inclusiveAmount == null) return BigDecimal.ZERO;
        BigDecimal amt = inclusiveAmount.setScale(2, RoundingMode.HALF_UP);
        return amt.subtract(taxableValue(amt, ratePercent));
    }
}
