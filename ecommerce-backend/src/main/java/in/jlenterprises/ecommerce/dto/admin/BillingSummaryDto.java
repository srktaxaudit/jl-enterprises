package in.jlenterprises.ecommerce.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;

/** Revenue &amp; GST summary for a date range (admin billing dashboard). */
public record BillingSummaryDto(
        Instant from,
        Instant to,
        long orderCount,
        BigDecimal grossRevenue,     // sum of grand totals, excluding cancelled
        BigDecimal taxCollected,     // GST embedded in successfully-paid orders
        BigDecimal netCollected,     // paid minus refunded
        BigDecimal paidTotal,
        BigDecimal pendingTotal,
        BigDecimal refundedTotal,
        BigDecimal codTotal,
        BigDecimal onlineTotal,
        long paidCount,
        long pendingCount,
        long refundedCount,
        BigDecimal gstRate
) {}
