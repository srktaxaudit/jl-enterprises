import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { apiGet } from "@/core/api/client";
import type { PageResponse } from "@/core/types";

/** Mirrors BillingSummaryDto (dto/billing/BillingSummaryDto.java). */
export interface BillingSummary {
  from?: string;
  to?: string;
  orderCount: number;
  grossRevenue: number;
  taxCollected: number;
  netCollected: number;
  paidTotal: number;
  pendingTotal: number;
  refundedTotal: number;
  codTotal: number;
  onlineTotal: number;
  paidCount: number;
  pendingCount: number;
  refundedCount: number;
  gstRate: number;
}

/** Mirrors BillingRowDto (dto/billing/BillingRowDto.java). */
export interface BillingRow {
  orderId: string;
  orderNumber: string;
  customerName?: string;
  customerEmail?: string;
  placedAt?: string;
  orderStatus: string;
  paymentMethod?: string;
  paymentStatus?: string;
  grandTotal: number;
  taxTotal: number;
  currency?: string;
}

export function useBillingSummary() {
  return useQuery({ queryKey: ["billing", "summary"], queryFn: () => apiGet<BillingSummary>("/api/v1/admin/billing/summary") });
}

export function useInvoices() {
  return useInfiniteQuery({
    queryKey: ["billing", "invoices"],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<BillingRow>>("/api/v1/admin/billing/invoices", { page: pageParam, size: 20 }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}

export function payTone(s?: string): "info" | "success" | "warn" | "danger" | "muted" {
  switch (s) {
    case "SUCCESS":
    case "PAID":
      return "success";
    case "REFUNDED":
      return "muted";
    case "FAILED":
      return "danger";
    case "PENDING":
      return "warn";
    default:
      return "info";
  }
}
