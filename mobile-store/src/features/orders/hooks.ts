import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost } from "@/core/api/client";
import { useAuth } from "@/core/auth/authStore";
import type { Order, OrderSummary, OrderTracking, PageResponse, PaymentMethod } from "@/core/types";

export function useMyOrders() {
  const authed = useAuth((s) => s.status === "authed");
  return useInfiniteQuery({
    queryKey: ["orders"],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<OrderSummary>>("/api/v1/orders", { page: pageParam, size: 20 }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
    enabled: authed,
  });
}

export function useOrder(id: string | undefined) {
  return useQuery({
    queryKey: ["orders", id],
    queryFn: () => apiGet<Order>(`/api/v1/orders/${id}`),
    enabled: !!id,
  });
}

export interface PlaceOrderInput {
  shippingAddressId: string;
  billingAddressId?: string;
  couponCode?: string;
  paymentMethod: PaymentMethod;
  notes?: string;
}

export function usePlaceOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: PlaceOrderInput) => apiPost<Order>("/api/v1/orders", input),
    onSuccess: () => {
      // Checkout consumes the server-side cart.
      void qc.invalidateQueries({ queryKey: ["cart"] });
      void qc.invalidateQueries({ queryKey: ["orders"] });
    },
  });
}

export function useCancelOrder(id: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (reason?: string) =>
      apiPost<Order>(`/api/v1/orders/${id}/cancel`, undefined, { reason }),
    onSuccess: (order) => {
      qc.setQueryData(["orders", id], order);
      void qc.invalidateQueries({ queryKey: ["orders"] });
    },
  });
}

export function useRequestReturn(id: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (reason?: string) =>
      apiPost<Order>(`/api/v1/orders/${id}/return`, undefined, { reason }),
    onSuccess: (order) => {
      qc.setQueryData(["orders", id], order);
      void qc.invalidateQueries({ queryKey: ["orders"] });
    },
  });
}

/** Public order tracking by order number + phone (works signed-out). */
export function useTrackOrder() {
  return useMutation({
    mutationFn: ({ number, phone }: { number: string; phone: string }) =>
      apiGet<OrderTracking>("/api/v1/orders/track", { number, phone }),
  });
}
