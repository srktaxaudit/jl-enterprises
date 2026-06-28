import { NextResponse } from "next/server";
import { createRazorpayOrder, isRazorpayConfigured } from "@/lib/razorpay";

/** POST /api/razorpay/order — create a Razorpay order for the checkout modal.
 *  Returns { demo:true } when keys aren't set so checkout can fall back. */
export async function POST(req: Request) {
  const { amount } = await req.json().catch(() => ({ amount: 0 }));

  if (!isRazorpayConfigured()) {
    return NextResponse.json({ demo: true });
  }
  try {
    const order = await createRazorpayOrder(Number(amount), "rcpt_" + Date.now());
    return NextResponse.json(order);
  } catch (e) {
    return NextResponse.json({ error: String(e) }, { status: 500 });
  }
}
