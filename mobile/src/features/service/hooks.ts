import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPatch } from "@/core/api/client";
import type { PageResponse } from "@/core/types";

/** Mirrors ServiceBookingDto (dto/service/ServiceBookingDto.java). */
export interface ServiceBooking {
  id: string;
  customerName: string;
  phone: string;
  serviceType: string;
  message?: string;
  preferredDate?: string;
  bookingStatus: string; // NEW | CONTACTED | SCHEDULED | DONE | CANCELLED
  createdAt?: string;
}

const PAGE_SIZE = 20;

/** Paginated service bookings (GET /api/v1/service-bookings, staff). */
export function useServiceBookings() {
  return useInfiniteQuery({
    queryKey: ["service", "list"],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<ServiceBooking>>("/api/v1/service-bookings", { page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}

/** Update a booking's status (PATCH /api/v1/service-bookings/{id}/status?status=). */
export function useUpdateBookingStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: string; status: string }) =>
      apiPatch(`/api/v1/service-bookings/${v.id}/status`, undefined, { status: v.status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["service"] }),
  });
}

// Matches the web admin (admin-service.html).
export const SERVICE_STATUSES = ["NEW", "CONTACTED", "SCHEDULED", "DONE", "CANCELLED"] as const;

export function serviceTone(s: string): "info" | "success" | "warn" | "danger" | "muted" {
  switch (s) {
    case "DONE":
      return "success";
    case "CANCELLED":
      return "danger";
    case "CONTACTED":
    case "SCHEDULED":
      return "warn";
    case "NEW":
      return "info";
    default:
      return "muted";
  }
}
