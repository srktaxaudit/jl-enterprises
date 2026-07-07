import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPatch } from "@/core/api/client";
import type { PageResponse } from "@/core/types";

/** Mirrors ExchangeRequestDto (dto/exchange/ExchangeRequestDto.java). */
export interface ExchangeRequest {
  id: string;
  customerName: string;
  customerEmail?: string;
  applianceCategory: string;
  brand?: string;
  modelNumber?: string;
  purchaseYear?: number;
  conditionGrade?: string;
  working: boolean;
  reason?: string;
  imageUrls?: string[];
  expectedValue?: number;
  estimatedValue?: number;
  finalValue?: number;
  desiredProductId?: string;
  desiredProductName?: string;
  status: string; // ExchangeStatus
  internalNotes?: string;
  appliedOrderId?: string;
  createdAt?: string;
  updatedAt?: string;
}

const PAGE_SIZE = 20;

/** Paginated exchange requests (GET /api/v1/admin/exchanges, newest first). */
export function useExchanges() {
  return useInfiniteQuery({
    queryKey: ["exchanges", "list"],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<ExchangeRequest>>("/api/v1/admin/exchanges", { page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}

/** Update status (PATCH /api/v1/admin/exchanges/{id}/status?status=). */
export function useUpdateExchangeStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; status: string }) =>
      apiPatch(`/api/v1/admin/exchanges/${v.id}/status`, undefined, { status: v.status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["exchanges"] }),
  });
}

// Actionable statuses (the transition guard on the backend rejects illegal jumps).
export const EXCHANGE_STATUSES = [
  "UNDER_REVIEW",
  "INSPECTION_SCHEDULED",
  "OFFER_SENT",
  "APPROVED",
  "REJECTED",
  "CANCELLED",
] as const;

export function exchangeTone(s: string): "info" | "success" | "warn" | "danger" | "muted" {
  switch (s) {
    case "APPROVED":
    case "COMPLETED":
      return "success";
    case "REJECTED":
    case "CANCELLED":
      return "danger";
    case "OFFER_SENT":
    case "INSPECTION_SCHEDULED":
    case "UNDER_REVIEW":
      return "warn";
    default:
      return "info";
  }
}
