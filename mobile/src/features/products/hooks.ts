import { useInfiniteQuery } from "@tanstack/react-query";
import { apiGet } from "@/core/api/client";
import type { PageResponse } from "@/core/types";

export interface ProductSummary {
  id: string;
  name: string;
  slug: string;
  sku?: string;
  price: number;
  comparePrice?: number;
  primaryImageUrl?: string;
  brandName?: string;
  categorySlug?: string;
  averageRating?: number;
  reviewCount?: number;
}

const PAGE_SIZE = 20;

export function useProducts(search?: string) {
  return useInfiniteQuery({
    queryKey: ["products", "list", search ?? ""],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<ProductSummary>>("/api/v1/admin/products", {
        page: pageParam,
        size: PAGE_SIZE,
        ...(search ? { search } : {}),
      }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}
