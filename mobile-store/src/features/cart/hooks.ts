import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiDelete, apiGet, apiPost, apiPut } from "@/core/api/client";
import { useAuth } from "@/core/auth/authStore";
import type { Cart, Coupon, CouponValidationResult } from "@/core/types";

const KEY = ["cart"];

/** The cart lives server-side and needs a signed-in customer. */
export function useCart() {
  const authed = useAuth((s) => s.status === "authed");
  return useQuery({
    queryKey: KEY,
    queryFn: () => apiGet<Cart>("/api/v1/cart"),
    enabled: authed,
  });
}

export function useAddToCart() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { productId: string; variantId?: string; quantity: number }) =>
      apiPost<Cart>("/api/v1/cart/items", input),
    onSuccess: (cart) => qc.setQueryData(KEY, cart),
  });
}

export function useUpdateCartItem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, quantity }: { itemId: string; quantity: number }) =>
      apiPut<Cart>(`/api/v1/cart/items/${itemId}`, { quantity }),
    onSuccess: (cart) => qc.setQueryData(KEY, cart),
  });
}

export function useRemoveCartItem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (itemId: string) => apiDelete<Cart>(`/api/v1/cart/items/${itemId}`),
    onSuccess: (cart) => qc.setQueryData(KEY, cart),
  });
}

export function useClearCart() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiDelete<void>("/api/v1/cart"),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
}

/** Coupons the signed-in customer can apply right now. */
export function useEligibleCoupons(enabled: boolean) {
  return useQuery({
    queryKey: ["coupons", "eligible"],
    queryFn: () => apiGet<Coupon[]>("/api/v1/coupons/eligible"),
    enabled,
  });
}

export function useValidateCoupon() {
  return useMutation({
    mutationFn: (code: string) =>
      apiGet<CouponValidationResult>("/api/v1/coupons/validate", { code }),
  });
}
