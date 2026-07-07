import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPut } from "@/core/api/client";

/** Mirrors InventoryDto (dto/inventory/InventoryDto.java). */
export interface InventoryItem {
  productId: string;
  productName: string;
  quantity: number;
  reserved: number;
  available: number;
  reorderLevel: number;
  warehouseLocation?: string;
  stockStatus: string; // IN_STOCK | LOW_STOCK | OUT_OF_STOCK
}

/** Products at or below their reorder level (GET /api/v1/admin/inventory/low-stock — plain list). */
export function useLowStock() {
  return useQuery({
    queryKey: ["inventory", "low-stock"],
    queryFn: () => apiGet<InventoryItem[]>("/api/v1/admin/inventory/low-stock"),
  });
}

/** PUT /api/v1/admin/inventory/{productId} — set quantity, reorder level and location. */
export function useUpdateInventory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { productId: string; quantity: number; reorderLevel: number; warehouseLocation?: string }) =>
      apiPut(`/api/v1/admin/inventory/${v.productId}`, {
        quantity: v.quantity,
        reorderLevel: v.reorderLevel,
        warehouseLocation: v.warehouseLocation ?? "",
      }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["inventory"] }),
  });
}

export function stockTone(s: string): "success" | "warn" | "danger" | "muted" {
  return s === "OUT_OF_STOCK" ? "danger" : s === "LOW_STOCK" ? "warn" : "success";
}
