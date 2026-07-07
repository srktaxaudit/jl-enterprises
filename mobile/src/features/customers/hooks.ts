import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPatch } from "@/core/api/client";
import type { PageResponse } from "@/core/types";

/** Mirrors UserDto (dto/auth/UserDto.java). */
export interface UserRow {
  id: string;
  email: string;
  phone?: string;
  firstName?: string;
  lastName?: string;
  department?: string;
  designation?: string;
  enabled: boolean;
  emailVerified: boolean;
  phoneVerified: boolean;
  lastLoginAt?: string;
  roles: string[];
}

const PAGE_SIZE = 20;

/** Paginated users (GET /api/v1/admin/users?search=). */
export function useCustomers(search?: string) {
  return useInfiniteQuery({
    queryKey: ["customers", "list", search ?? ""],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<UserRow>>("/api/v1/admin/users", {
        page: pageParam,
        size: PAGE_SIZE,
        ...(search ? { search } : {}),
      }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}

/** Enable / disable a user (PATCH /api/v1/admin/users/{id}/status?enabled=). */
export function useSetUserEnabled() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; enabled: boolean }) =>
      apiPatch(`/api/v1/admin/users/${v.id}/status`, undefined, { enabled: v.enabled }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["customers"] }),
  });
}

export const fullName = (u: UserRow): string =>
  [u.firstName, u.lastName].filter(Boolean).join(" ").trim() || u.email;

export const bareRoles = (u: UserRow): string =>
  (u.roles ?? []).map((r) => r.replace("ROLE_", "")).join(", ");
