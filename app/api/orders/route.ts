import { NextResponse } from "next/server";
import { createOrder } from "@/lib/orders";
import { priceOrder } from "@/lib/pricing";
import { validateContact, validateDelivery, validateEmail } from "@/lib/checkout";
import { getSessionUser } from "@/lib/supabase/server";

/** POST /api/orders — COD / EMI orders. Razorpay orders go through
 *  /api/razorpay/verify after payment. Demo order returned when no DB.
 *  Totals are recomputed server-side — client-sent amounts are ignored. */
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));

  const paymentMode = body.paymentMode === "EMI" ? "EMI" : "COD";

  const contact = validateContact(body.contact);
  if (!contact.ok) {
    return NextResponse.json({ error: contact.error }, { status: 400 });
  }

  const email = validateEmail(body.contact?.email);
  if (!email.ok) {
    return NextResponse.json({ error: email.error }, { status: 400 });
  }

  const delivery = validateDelivery(body.delivery);
  if (!delivery.ok) {
    return NextResponse.json({ error: delivery.error }, { status: 400 });
  }

  const priced = await priceOrder(body.items);
  if (!priced.ok) {
    return NextResponse.json({ error: priced.error }, { status: 400 });
  }

  const user = await getSessionUser();

  const result = await createOrder({
    items: priced.items,
    contact: contact.contact,
    paymentMode,
    paymentStatus: "PENDING",
    subtotal: priced.subtotal,
    discount: priced.discount,
    total: priced.total,
    email: email.email ?? user?.email ?? null,
    userId: user?.id ?? null,
    delivery: { date: delivery.date, slot: delivery.slot },
  });

  if ("error" in result) {
    return NextResponse.json(result, { status: 500 });
  }
  return NextResponse.json(result);
}
