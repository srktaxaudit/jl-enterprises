import { NextResponse } from "next/server";
import { createRazorpayOrder, isRazorpayConfigured } from "@/lib/razorpay";
import { priceOrder } from "@/lib/pricing";

/** POST /api/razorpay/order — create a Razorpay order for the checkout modal.
 *  The amount is computed server-side from the cart's product ids, never
 *  taken from the client. Returns { demo:true } when keys aren't set. */
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));

  if (!isRazorpayConfigured()) {
    return NextResponse.json({ demo: true });
  }

  const priced = await priceOrder(body.items);
  if (!priced.ok) {
    return NextResponse.json({ error: priced.error }, { status: 400 });
  }
  if (priced.total <= 0) {
    return NextResponse.json({ error: "Nothing to pay for this order." }, { status: 400 });
  }

  try {
    const order = await createRazorpayOrder(priced.total, "rcpt_" + Date.now());
    return NextResponse.json(order);
  } catch (e) {
    return NextResponse.json({ error: String(e) }, { status: 500 });
  }
}
