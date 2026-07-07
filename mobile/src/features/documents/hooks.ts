import { useInfiniteQuery } from "@tanstack/react-query";
import { apiGet } from "@/core/api/client";
import type { PageResponse } from "@/core/types";

/** Mirrors DocumentSummaryDto (dto/document/DocumentSummaryDto.java). */
export interface DocumentSummary {
  id: string;
  documentNumber: string;
  documentDate?: string;
  partyName?: string;
  taxableTotal: number;
  gstTotal: number;
  grandTotal: number;
  documentType?: string;
  status?: string;
}

/** Paginated invoices & bills (GET /api/v1/admin/documents). */
export function useDocuments() {
  return useInfiniteQuery({
    queryKey: ["documents", "list"],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<DocumentSummary>>("/api/v1/admin/documents", { page: pageParam, size: 20 }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}
