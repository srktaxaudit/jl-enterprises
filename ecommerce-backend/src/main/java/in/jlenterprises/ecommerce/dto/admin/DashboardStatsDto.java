package in.jlenterprises.ecommerce.dto.admin;

import java.math.BigDecimal;

/** Aggregated figures for the admin dashboard. */
public record DashboardStatsDto(
        long totalUsers,
        long totalProducts,
        long totalOrders,
        long pendingOrders,
        long lowStockCount,
        long ordersLast30Days,
        BigDecimal revenueLast30Days
) {}
