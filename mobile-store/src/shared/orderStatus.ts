import type { OrderStatus } from "@/core/types";

/** Badge tone for an order status, shared by the orders list and detail. */
export function statusTone(status: OrderStatus): "info" | "success" | "warn" | "danger" | "muted" {
  switch (status) {
    case "DELIVERED":
      return "success";
    case "CANCELLED":
    case "FAILED_PAYMENT":
      return "danger";
    case "RETURN_REQUESTED":
    case "RETURNED":
    case "REFUNDED":
      return "warn";
    case "PENDING":
      return "muted";
    default:
      return "info";
  }
}
