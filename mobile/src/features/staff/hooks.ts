import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPatch, apiPost } from "@/core/api/client";
import type { PageResponse } from "@/core/types";
import type { UserRow } from "@/features/customers/hooks";

// Staff accounts share the UserDto shape used for customers.
export type StaffRow = UserRow;

const PAGE_SIZE = 50;

/** Paginated staff accounts (GET /api/v1/admin/staff?search=). */
export function useStaff(search?: string) {
  return useInfiniteQuery({
    queryKey: ["staff", "list", search ?? ""],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<StaffRow>>("/api/v1/admin/staff", {
        page: pageParam,
        size: PAGE_SIZE,
        ...(search ? { search } : {}),
      }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}

/** Activate / deactivate a staff account (PATCH /api/v1/admin/staff/{id}/status?enabled=). */
export function useSetStaffEnabled() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; enabled: boolean }) =>
      apiPatch(`/api/v1/admin/staff/${v.id}/status`, undefined, { enabled: v.enabled }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["staff"] }),
  });
}

/** Reset a staff member's password (POST /api/v1/admin/staff/{id}/reset-password). */
export function useResetStaffPassword() {
  return useMutation({
    mutationFn: (v: { id: string; password: string }) =>
      apiPost(`/api/v1/admin/staff/${v.id}/reset-password`, { password: v.password }),
  });
}
