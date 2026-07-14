import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiDelete, apiGet, apiPost } from "@/core/api/client";
import { useAuth } from "@/core/auth/authStore";
import type { Wishlist } from "@/core/types";

const KEY = ["wishlist"];

export function useWishlist() {
  const authed = useAuth((s) => s.status === "authed");
  return useQuery({
    queryKey: KEY,
    queryFn: () => apiGet<Wishlist>("/api/v1/wishlist"),
    enabled: authed,
  });
}

export function useToggleWishlist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ productId, inWishlist }: { productId: string; inWishlist: boolean }) =>
      inWishlist
        ? apiDelete<Wishlist>(`/api/v1/wishlist/items/${productId}`)
        : apiPost<Wishlist>(`/api/v1/wishlist/items/${productId}`),
    onSuccess: (wishlist) => qc.setQueryData(KEY, wishlist),
  });
}
