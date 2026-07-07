import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPut } from "@/core/api/client";

/** Mirrors SettingDto (dto/admin/SettingDto.java). */
export interface Setting {
  key: string;
  value: string;
  updatedAt?: string;
}

/** All business settings (GET /api/v1/admin/settings). */
export function useSettings() {
  return useQuery({
    queryKey: ["settings", "list"],
    queryFn: () => apiGet<Setting[]>("/api/v1/admin/settings"),
  });
}

/** Create or update a setting (PUT /api/v1/admin/settings/{key} with { value }). */
export function useUpsertSetting() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { key: string; value: string }) =>
      apiPut(`/api/v1/admin/settings/${encodeURIComponent(v.key)}`, { value: v.value }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["settings"] }),
  });
}
