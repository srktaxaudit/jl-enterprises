import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/core/api/client";

/** Shape returned by GET /api/v1/admin/dashboard/stats (see AdminDashboardController). */
export interface DashboardStats {
  revenueLast30Days: number;
  ordersLast30Days: number;
  totalOrders: number;
  pendingOrders: number;
  processingOrders?: number;
  deliveredOrders?: number;
  cancelledOrders?: number;
  returnedOrders?: number;
  totalProducts: number;
  totalInventoryItems?: number;
  inStockCount?: number;
  lowStockCount: number;
  outOfStockCount?: number;
  totalUsers: number;
}

export function useDashboardStats() {
  return useQuery({
    queryKey: ["dashboard", "stats"],
    queryFn: () => apiGet<DashboardStats>("/api/v1/admin/dashboard/stats"),
  });
}
