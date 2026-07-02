import { NextResponse } from "next/server";
import { createAdminClient } from "@/lib/supabase/admin";
import { normalisePhone } from "@/lib/whatsapp";

/** POST /api/track — look up an order by order number + phone.
 *  The phone must match the one on the order, so order numbers alone
 *  can't be used to read other customers' details. */
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const orderNo = String(body.orderNo ?? "").trim().toUpperCase();
  const phone = normalisePhone(String(body.phone ?? ""));

  if (!/^JL\d{4,10}$/.test(orderNo) || !phone) {
    return NextResponse.json(
      { error: "Enter your order number (e.g. JL123456) and the phone used at checkout." },
      { status: 400 }
    );
  }

  const admin = createAdminClient();
  if (!admin) {
    // Demo mode — show a sample order so the page can be previewed.
    return NextResponse.json({
      demo: true,
      order: {
        orderNo,
        status: "OUT_FOR_DELIVERY",
        paymentMode: "COD",
        paymentStatus: "PENDING",
        total: 34990,
        createdAt: new Date().toISOString(),
        deliveryDate: null,
        deliverySlot: null,
        city: "Thoothukudi",
        items: [{ name: "1.5 Ton 3-Star Inverter Split AC", brand: "Voltas", qty: 1, price: 34990 }],
        returnRequest: null,
      },
    });
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

  const { data: items } = await admin
    .from("order_items")
    .select("name, brand, qty, price")
    .eq("order_id", order.id);

  // return_requests exists only after schema-v2 — treat errors as "none".
  let returnRequest: { kind: string; status: string; createdAt: string } | null = null;
  try {
    const { data: rr } = await admin
      .from("return_requests")
      .select("kind, status, created_at")
      .eq("order_id", order.id)
      .order("created_at", { ascending: false })
      .limit(1)
      .maybeSingle();
    if (rr) returnRequest = { kind: rr.kind, status: rr.status, createdAt: rr.created_at };
  } catch {}

  return NextResponse.json({
    order: {
      orderNo: order.order_no,
      status: order.status,
      paymentMode: order.payment_mode,
      paymentStatus: order.payment_status,
      total: Number(order.total),
      createdAt: order.created_at,
      deliveryDate: order.delivery_date ?? null,
      deliverySlot: order.delivery_slot ?? null,
      city: order.city,
      items: (items ?? []).map((i) => ({
        name: i.name,
        brand: i.brand,
        qty: i.qty,
        price: Number(i.price),
      })),
      returnRequest,
    },
  });
}
