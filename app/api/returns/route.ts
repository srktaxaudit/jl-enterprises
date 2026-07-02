import { NextResponse } from "next/server";
import { createAdminClient } from "@/lib/supabase/admin";
import { normalisePhone, sendText } from "@/lib/whatsapp";
import { sendReturnEmailStatus } from "@/lib/email";

const OPEN_STATUSES = ["REQUESTED", "APPROVED", "PICKED_UP"];

/** POST /api/returns — customer return / cancellation request.
 *  Verified the same way as tracking: order number + matching phone. */
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const orderNo = String(body.orderNo ?? "").trim().toUpperCase();
  const phone = normalisePhone(String(body.phone ?? ""));
  const kind = body.kind === "CANCEL" ? "CANCEL" : "RETURN";
  const reason = String(body.reason ?? "").trim();

  if (!/^JL\d{4,10}$/.test(orderNo) || !phone) {
    return NextResponse.json({ error: "Invalid order number or phone." }, { status: 400 });
  }
  if (reason.length < 5 || reason.length > 500) {
    return NextResponse.json(
      { error: "Please describe the reason in a few words (at least 5 characters)." },
      { status: 400 }
    );
  }

  const admin = createAdminClient();
  if (!admin) {
    return NextResponse.json({ ok: true, demo: true, status: "REQUESTED" });
  }

  const { data: order } = await admin
    .from("orders")
    .select("*")
    .eq("order_no", orderNo)
    .maybeSingle();

  if (!order || normalisePhone(order.contact_phone) !== phone) {
    return NextResponse.json(
      { error: "No order found for that order number and phone." },
      { status: 404 }
    );
  }

  if (kind === "CANCEL" && !["NEW", "PACKED"].includes(order.status)) {
    return NextResponse.json(
      { error: "This order is already on its way — request a return instead once it's delivered." },
      { status: 400 }
    );
  }
  if (kind === "RETURN" && order.status !== "DELIVERED") {
    return NextResponse.json(
      { error: "Returns can be requested once the order is delivered. For orders in transit, request a cancellation." },
      { status: 400 }
    );
  }
  if (order.status === "CANCELLED") {
    return NextResponse.json({ error: "This order is already cancelled." }, { status: 400 });
  }

  const { data: existing } = await admin
    .from("return_requests")
    .select("id, status")
    .eq("order_id", order.id)
    .in("status", OPEN_STATUSES)
    .limit(1)
    .maybeSingle();
  if (existing) {
    return NextResponse.json(
      { error: "A request for this order is already open — we'll contact you shortly." },
      { status: 400 }
    );
  }

  const { error } = await admin.from("return_requests").insert({
    order_id: order.id,
    order_no: order.order_no,
    phone: order.contact_phone,
    kind,
    reason,
  });
  if (error) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }

  const label = kind === "CANCEL" ? "cancellation" : "return";
  await sendText(
    order.contact_phone,
    `JL Enterprises — we've received your ${label} request for order *#${order.order_no}*. Our team will confirm within 1 working day. 🙏`
  );
  await sendReturnEmailStatus({
    email: order.email ?? null,
    orderNo: order.order_no,
    kind,
    status: "REQUESTED",
  });

  return NextResponse.json({ ok: true, status: "REQUESTED" });
}
