import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/core/api/client";

/** Mirrors CouponDto (dto/coupon/CouponDto.java). */
export interface Coupon {
  id: string;
  code: string;
  name?: string;
  description?: string;
  type: string; // PERCENTAGE | FIXED (CouponType)
  value: number;
  minOrderAmount?: number;
  maxDiscount?: number;
  usageLimit?: number;
  usedCount: number;
  perUserLimit?: number;
  firstOrderOnly: boolean;
  startsAt?: string;
  expiresAt?: string;
  active: boolean;
  allCategories: boolean;
}

/** All coupons (GET /api/v1/coupons — plain list, staff-visible). */
export function useCoupons() {
  return useQuery({
    queryKey: ["coupons", "list"],
    queryFn: () => apiGet<Coupon[]>("/api/v1/coupons"),
  });
}

/** Human label for the discount value. */
export function couponValue(c: Coupon): string {
  return c.type === "PERCENTAGE" ? `${Math.round(c.value)}% off` : `₹${Math.round(c.value)} off`;
}

/** Live / scheduled / expired / disabled, for the badge. */
export function couponState(c: Coupon): { label: string; tone: "success" | "warn" | "muted" | "danger" } {
  if (!c.active) return { label: "Disabled", tone: "muted" };
  const now = Date.now();
  if (c.startsAt && new Date(c.startsAt).getTime() > now) return { label: "Scheduled", tone: "warn" };
  if (c.expiresAt && new Date(c.expiresAt).getTime() < now) return { label: "Expired", tone: "danger" };
  return { label: "Live", tone: "success" };
}
