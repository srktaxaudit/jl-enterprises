import { NextResponse } from "next/server";
import { createOrder } from "@/lib/orders";

/** POST /api/orders — COD / EMI orders. Razorpay orders go through
 *  /api/razorpay/verify after payment. Demo order returned when no DB. */
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const result = await createOrder({
    items: body.items ?? [],
    contact: body.contact ?? {},
    paymentMode: body.paymentMode ?? "COD",
    paymentStatus: "PENDING",
    subtotal: body.subtotal ?? 0,
    discount: body.discount ?? 0,
    total: body.total ?? 0,
  });

  if ("error" in result) {
    return NextResponse.json(result, { status: 500 });
  }
  return NextResponse.json(result);
}
