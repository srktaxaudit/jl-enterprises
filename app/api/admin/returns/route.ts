import { NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";
import { createAdminClient } from "@/lib/supabase/admin";
import { refundRazorpayPayment } from "@/lib/razorpay";
import { sendText } from "@/lib/whatsapp";
import { sendReturnEmailStatus } from "@/lib/email";

const FLOW = ["REQUESTED", "APPROVED", "REJECTED", "PICKED_UP", "REFUNDED"];

const CUSTOMER_MSG: Record<string, string> = {
  APPROVED: "has been approved — our team will contact you to arrange pickup",
  REJECTED: "could not be approved — we'll call you to explain",
  PICKED_UP: "item has been picked up — your refund is being processed",
  REFUNDED: "has been refunded — the amount will reflect in 5–7 business days",
};

/** Add order items back to stock (decrement with a negative qty). */
async function restock(sb: any, orderId: string) {
  const { data: items } = await sb
    .from("order_items")
    .select("product_id, qty")
    .eq("order_id", orderId);
  for (const i of items ?? []) {
    if (!i.product_id) continue;
    await sb.rpc("decrement_stock", { p_id: i.product_id, p_qty: -Number(i.qty || 0) }).then(
      () => {},
      () => {}
    );
  }
}

/** POST /api/admin/returns — advance a return/cancellation request.
 *  Side effects: approving a CANCEL cancels the order and restocks;
 *  PICKED_UP restocks; REFUNDED triggers a Razorpay refund for prepaid orders. */
export async function POST(req: Request) {
  if (!isAdmin()) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id, status } = await req.json().catch(() => ({}));
  if (!id || !FLOW.includes(status)) {
    return NextResponse.json({ error: "id and a valid status required" }, { status: 400 });
  }

  const sb = createAdminClient();
  if (!sb) return NextResponse.json({ ok: true, demo: true });

  const { data: rr, error: rrErr } = await sb
    .from("return_requests")
    .select("*, order:orders(id, order_no, status, payment_mode, payment_ref, total, email, contact_phone)")
    .eq("id", id)
    .single();
  if (rrErr || !rr) {
    return NextResponse.json({ error: rrErr?.message ?? "Request not found" }, { status: 404 });
  }
  const order = rr.order;
  const prevStatus = rr.status;

  const { error } = await sb
    .from("return_requests")
    .update({ status, updated_at: new Date().toISOString() })
    .eq("id", id);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });

  let note = "";

  // Approving a cancellation → cancel the order and put stock back.
  if (rr.kind === "CANCEL" && status === "APPROVED" && order && order.status !== "CANCELLED") {
    await sb.from("orders").update({ status: "CANCELLED" }).eq("id", order.id);
    await restock(sb, order.id);
    note = "Order cancelled and stock restored.";
  }

  // Returned item collected → put stock back.
  if (rr.kind === "RETURN" && status === "PICKED_UP" && prevStatus !== "PICKED_UP" && order) {
    await restock(sb, order.id);
    note = "Stock restored.";
  }

  // Refund: automatic for Razorpay payments, manual reminder otherwise.
  if (status === "REFUNDED" && order) {
    if (order.payment_mode === "RAZORPAY" && order.payment_ref) {
      const refund = await refundRazorpayPayment(order.payment_ref, Number(order.total) * 100);
      note = refund.ok
        ? `Razorpay refund initiated (${refund.refundId}).`
        : `⚠️ Razorpay refund failed: ${refund.error} — refund manually from the Razorpay dashboard.`;
    } else {
      note = "COD/EMI order — transfer the refund to the customer's bank manually.";
    }
  }

  if (order) {
    const label = rr.kind === "CANCEL" ? "cancellation" : "return";
    const human = CUSTOMER_MSG[status];
    if (human) {
      await sendText(
        order.contact_phone,
        `JL Enterprises — your ${label} request for order *#${order.order_no}* ${human}. 🙏`
      );
      await sendReturnEmailStatus({ email: order.email ?? null, orderNo: order.order_no, kind: rr.kind, status });
    }
  }

  return NextResponse.json({ ok: true, note });
}
