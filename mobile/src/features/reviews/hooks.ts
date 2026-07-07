import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPatch } from "@/core/api/client";
import type { PageResponse } from "@/core/types";

/** Mirrors ReviewDto (dto/review/ReviewDto.java). */
export interface Review {
  id: string;
  productId: string;
  reviewerName: string;
  rating: number;
  title?: string;
  comment?: string;
  status: string; // PENDING | APPROVED | REJECTED (ReviewStatus)
  verifiedPurchase: boolean;
  createdAt?: string;
}

const PAGE_SIZE = 20;

/** Reviews filtered by status (GET /api/v1/admin/reviews?status=PENDING). */
export function useReviews(status: string) {
  return useInfiniteQuery({
    queryKey: ["reviews", "list", status],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<Review>>("/api/v1/admin/reviews", { status, page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}

/** Approve or reject a review (PATCH /api/v1/admin/reviews/{id}/moderate?status=). */
export function useModerateReview() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; status: "APPROVED" | "REJECTED" }) =>
      apiPatch(`/api/v1/admin/reviews/${v.id}/moderate`, undefined, { status: v.status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["reviews"] }),
  });
}

export const REVIEW_TABS = ["PENDING", "APPROVED", "REJECTED"] as const;

export function reviewTone(s: string): "info" | "success" | "danger" | "muted" {
  return s === "APPROVED" ? "success" : s === "REJECTED" ? "danger" : "info";
}
