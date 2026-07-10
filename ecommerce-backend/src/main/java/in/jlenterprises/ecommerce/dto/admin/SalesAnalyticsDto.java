package in.jlenterprises.ecommerce.dto.admin;

import java.math.BigDecimal;
import java.util.List;

/** Sales analytics over a rolling window: revenue trend, best-sellers, category mix. */
public record SalesAnalyticsDto(
        int days,
        BigDecimal totalRevenue,
        long orderCount,
        BigDecimal avgOrderValue,
        List<TrendPoint> revenueTrend,
        List<TopProduct> topProducts,
        List<CategorySlice> categoryBreakdown
) {
    /** One day of the revenue trend (date = ISO yyyy-MM-dd). */
    public record TrendPoint(String date, BigDecimal revenue) {}
    /** A best-selling product by units sold. */
    public record TopProduct(String name, long quantity, BigDecimal revenue) {}
    /** Revenue attributed to a category. */
    public record CategorySlice(String category, BigDecimal revenue) {}
}
