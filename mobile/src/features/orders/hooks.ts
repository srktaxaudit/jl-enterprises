import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPatch } from "@/core/api/client";
import type { PageResponse } from "@/core/types";

export interface OrderSummary {
  id: string;
  orderNumber: string;
  status: string;
  grandTotal: number;
  itemCount: number;
  placedAt?: string;
  createdAt?: string;
}

const PAGE_SIZE = 20;

/** Paginated orders for infinite scroll + pull-to-refresh. */
export function useOrders() {
  return useInfiniteQuery({
    queryKey: ["orders", "list"],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<OrderSummary>>("/api/v1/admin/orders", { page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}

/**
 * Update an order's status. AdminOrderController declares
 * `@PatchMapping("/{id}/status")` with `@RequestParam OrderStatus status` — the
 * status goes in the QUERY STRING, not the JSON body (a body here 400s), matching
 * the web admin's `/status?status=...` call.
 */
export function useUpdateOrderStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      apiPatch(`/api/v1/admin/orders/${id}/status`, undefined, { status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["orders"] }),
  });
}

/** Map an OrderStatus to a StatusBadge tone (mirrors the web colour scheme). */
export function orderTone(status: string): "info" | "success" | "warn" | "danger" | "muted" {
  switch (status) {
    case "DELIVERED":
      return "success";
    case "CANCELLED":
    case "FAILED_PAYMENT":
      return "danger";
    case "PACKED":
    case "SHIPPED":
    case "OUT_FOR_DELIVERY":
    case "RETURN_REQUESTED":
      return "warn";
    case "REFUNDED":
    case "RETURNED":
      return "muted";
    default:
      return "info";
  }
}
