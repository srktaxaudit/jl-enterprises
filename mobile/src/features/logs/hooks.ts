import { useInfiniteQuery } from "@tanstack/react-query";
import { apiGet } from "@/core/api/client";
import type { PageResponse } from "@/core/types";

/** Mirrors AuditLogDto (dto/admin/AuditLogDto.java). */
export interface AuditLog {
  id: string;
  actor?: string;
  action: string;
  entity?: string;
  entityId?: string;
  detail?: string;
  ipAddress?: string;
  createdAt?: string;
}

const PAGE_SIZE = 50;

/** Paginated activity log, newest first (GET /api/v1/admin/audit-logs). */
export function useAuditLogs() {
  return useInfiniteQuery({
    queryKey: ["audit-logs", "list"],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<AuditLog>>("/api/v1/admin/audit-logs", { page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}
