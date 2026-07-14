import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost } from "@/core/api/client";
import type {
  Banner,
  Brand,
  Category,
  PageResponse,
  ProductDetail,
  ProductSummary,
  Review,
} from "@/core/types";

export interface ProductFilters {
  search?: string;
  category?: string;
  brand?: string;
  minPrice?: number;
  maxPrice?: number;
  sort?: string; // Spring pageable sort, e.g. "price,asc"
}

export function useBanners() {
  return useQuery({
    queryKey: ["banners"],
    queryFn: () => apiGet<Banner[]>("/api/v1/banners"),
    staleTime: 5 * 60_000,
  });
}

export function useCategories() {
  return useQuery({
    queryKey: ["categories"],
    queryFn: () => apiGet<Category[]>("/api/v1/categories"),
    staleTime: 10 * 60_000,
  });
}

export function useBrands() {
  return useQuery({
    queryKey: ["brands"],
    queryFn: () => apiGet<Brand[]>("/api/v1/brands"),
    staleTime: 10 * 60_000,
  });
}

export function useFeaturedProducts() {
  return useQuery({
    queryKey: ["products", "featured"],
    queryFn: () => apiGet<PageResponse<ProductSummary>>("/api/v1/products/featured", { size: 12 }),
    select: (page) => page.content,
  });
}

/** Infinite-scrolling product search for the Shop tab. */
export function useProductSearch(filters: ProductFilters) {
  return useInfiniteQuery({
    queryKey: ["products", "search", filters],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<ProductSummary>>("/api/v1/products", {
        page: pageParam,
        size: 20,
        search: filters.search || undefined,
        category: filters.category || undefined,
        brand: filters.brand || undefined,
        minPrice: filters.minPrice,
        maxPrice: filters.maxPrice,
        sort: filters.sort || undefined,
      }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}

export function useProduct(slug: string | undefined) {
  return useQuery({
    queryKey: ["product", slug],
    queryFn: () => apiGet<ProductDetail>(`/api/v1/products/${slug}`),
    enabled: !!slug,
  });
}

export function useRelatedProducts(slug: string | undefined) {
  return useQuery({
    queryKey: ["product", slug, "related"],
    queryFn: () => apiGet<ProductSummary[]>(`/api/v1/products/${slug}/related`, { limit: 8 }),
    enabled: !!slug,
  });
}

export function useProductReviews(productId: string | undefined) {
  return useQuery({
    queryKey: ["reviews", productId],
    queryFn: () => apiGet<PageResponse<Review>>(`/api/v1/products/${productId}/reviews`, { size: 10 }),
    enabled: !!productId,
    select: (page) => page.content,
  });
}

export function useSubmitReview(productId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { rating: number; title?: string; comment?: string }) =>
      apiPost<Review>(`/api/v1/products/${productId}/reviews`, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["reviews", productId] }),
  });
}
