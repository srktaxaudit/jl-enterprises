import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/core/api/client";

/** Mirrors RoleDto (dto/admin/RoleDto.java). */
export interface Role {
  id: string;
  name: string;
  description?: string;
  permissions?: string[];
}

/** All roles (GET /api/v1/admin/roles). */
export function useRoles() {
  return useQuery({
    queryKey: ["roles", "list"],
    queryFn: () => apiGet<Role[]>("/api/v1/admin/roles"),
  });
}
