import { NextResponse } from "next/server";
import { verifyRazorpaySignature } from "@/lib/razorpay";
import { createOrder } from "@/lib/orders";

/** POST /api/razorpay/verify — verify the payment signature, then persist
 *  the order as PAID and send the WhatsApp confirmation. */
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const {
    razorpay_order_id,
    razorpay_payment_id,
    razorpay_signature,
    order, // { items, contact, subtotal, discount, total }
  } = body;

  const ok = verifyRazorpaySignature({
    razorpay_order_id,
    razorpay_payment_id,
    razorpay_signature,
  });
  if (!ok) {
    return NextResponse.json({ error: "Payment verification failed" }, { status: 400 });
  }

  const result = await createOrder({
    items: order?.items ?? [],
    contact: order?.contact ?? {},
    paymentMode: "RAZORPAY",
    paymentStatus: "PAID",
    subtotal: order?.subtotal ?? 0,
    discount: order?.discount ?? 0,
    total: order?.total ?? 0,
  });

  if ("error" in result) {
    return NextResponse.json(result, { status: 500 });
  }
  return NextResponse.json({ ...result, paymentId: razorpay_payment_id });
}
