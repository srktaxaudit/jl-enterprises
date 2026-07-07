import { useMutation } from "@tanstack/react-query";
import { apiPost } from "@/core/api/client";

/** Mirrors BroadcastResult (dto/admin/BroadcastResult.java). */
export interface BroadcastResult {
  recipients: number;
  sent: number;
  failed: number;
  demoMode: boolean;
}

/** Broadcast a WhatsApp message (POST /api/v1/admin/whatsapp/broadcast). */
export function useBroadcast() {
  return useMutation({
    mutationFn: (v: { message: string; onlyVerified: boolean }) =>
      apiPost<BroadcastResult>("/api/v1/admin/whatsapp/broadcast", { message: v.message, onlyVerified: v.onlyVerified }),
  });
}
