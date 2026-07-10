package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.admin.DashboardStatsDto;
import in.jlenterprises.ecommerce.dto.admin.SalesAnalyticsDto;

public interface DashboardService {

    DashboardStatsDto getStats();

    /** Revenue trend, best-sellers and category breakdown over the last {@code days} days. */
    SalesAnalyticsDto getSalesAnalytics(int days);
}
