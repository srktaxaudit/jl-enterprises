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

    /**
     * Total GST embedded in a GST-inclusive order, computed PER LINE at each item's
     * snapshotted rate (electronics mix 18% and 28% slabs — one flat rate misstates the
     * liability). Coupon/exchange discounts shrink each line's share proportionally;
     * shipping and legacy lines without a snapshot use {@code defaultRate}.
     *
     * <p>Single source of truth for invoices AND journal postings — the printed invoice
     * and the books must never disagree.
     */
    public static BigDecimal embeddedGst(in.jlenterprises.ecommerce.entity.Order order,
                                         BigDecimal grand, BigDecimal defaultRate) {
        if (grand == null) return BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingTotal() == null ? BigDecimal.ZERO : order.getShippingTotal();
        BigDecimal merchandise = grand.subtract(shipping);
        if (merchandise.signum() < 0) merchandise = BigDecimal.ZERO;

        BigDecimal itemsTotal = order.getItems().stream()
                .map(i -> i.getLineTotal() == null ? BigDecimal.ZERO : i.getLineTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gst;
        if (itemsTotal.signum() <= 0) {
            gst = gstAmount(merchandise, defaultRate);
        } else {
            gst = BigDecimal.ZERO;
            for (var item : order.getItems()) {
                BigDecimal lineTotal = item.getLineTotal() == null ? BigDecimal.ZERO : item.getLineTotal();
                // This line's share of the actual consideration (post-discount/exchange).
                BigDecimal share = lineTotal.multiply(merchandise).divide(itemsTotal, 2, RoundingMode.HALF_UP);
                BigDecimal rate = item.getGstRate() != null ? item.getGstRate() : defaultRate;
                gst = gst.add(gstAmount(share, rate));
            }
        }
        return gst.add(gstAmount(shipping, defaultRate));
    }
}
