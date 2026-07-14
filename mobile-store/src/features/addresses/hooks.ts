import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiDelete, apiGet, apiPost, apiPut } from "@/core/api/client";
import { useAuth } from "@/core/auth/authStore";
import type { Address, AddressInput } from "@/core/types";

const KEY = ["addresses"];

export function useAddresses() {
  const authed = useAuth((s) => s.status === "authed");
  return useQuery({
    queryKey: KEY,
    queryFn: () => apiGet<Address[]>("/api/v1/addresses"),
    enabled: authed,
  });
}

export function useCreateAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: AddressInput) => apiPost<Address>("/api/v1/addresses", input),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
}

export function useUpdateAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: AddressInput }) =>
      apiPut<Address>(`/api/v1/addresses/${id}`, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
}

export function useDeleteAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiDelete<void>(`/api/v1/addresses/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
}

export function useSetDefaultAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiPut<Address>(`/api/v1/addresses/${id}/default`),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
}
