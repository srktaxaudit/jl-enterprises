import { NextResponse } from "next/server";
import { fetchRazorpayOrder, verifyRazorpaySignature } from "@/lib/razorpay";
import { createOrder } from "@/lib/orders";
import { priceOrder } from "@/lib/pricing";
import { validateContact, validateDelivery, validateEmail } from "@/lib/checkout";
import { getSessionUser } from "@/lib/supabase/server";

/** POST /api/razorpay/verify — verify the payment signature, cross-check the
 *  paid amount against the server-priced cart, then persist the order as
 *  PAID and send the WhatsApp confirmation. Client-sent totals are ignored. */
export async function POST(req: Request) {
  const body = await req.json().catch(() => ({}));
  const {
    razorpay_order_id,
    razorpay_payment_id,
    razorpay_signature,
    order, // { items, contact, delivery }
  } = body;

  const ok = verifyRazorpaySignature({
    razorpay_order_id,
    razorpay_payment_id,
    razorpay_signature,
  });
  if (!ok) {
    return NextResponse.json({ error: "Payment verification failed" }, { status: 400 });
  }

  const contact = validateContact(order?.contact);
  if (!contact.ok) {
    return NextResponse.json({ error: contact.error }, { status: 400 });
  }

  const email = validateEmail(order?.contact?.email);
  if (!email.ok) {
    return NextResponse.json({ error: email.error }, { status: 400 });
  }

  // Payment already happened — accept the order even if the slot is stale.
  const deliveryCheck = validateDelivery(order?.delivery);
  const delivery = deliveryCheck.ok
    ? { date: deliveryCheck.date, slot: deliveryCheck.slot }
    : null;

  // Payment is already captured — don't let a stock race void the order.
  const priced = await priceOrder(order?.items, { requireStock: false });
  if (!priced.ok) {
    return NextResponse.json({ error: priced.error }, { status: 400 });
  }

  // The signature proves this order/payment pair is genuine, but not that it
  // was for THIS cart — confirm the Razorpay order amount matches our pricing.
  const rzpOrder = await fetchRazorpayOrder(razorpay_order_id);
  if (!rzpOrder || rzpOrder.amountPaise !== Math.round(priced.total * 100)) {
    return NextResponse.json(
      { error: "Paid amount does not match the order — please contact the store." },
      { status: 400 }
    );
  }

  const user = await getSessionUser();

  const result = await createOrder({
    items: priced.items,
    contact: contact.contact,
    paymentMode: "RAZORPAY",
    paymentStatus: "PAID",
    subtotal: priced.subtotal,
    discount: priced.discount,
    total: priced.total,
    email: email.email ?? user?.email ?? null,
    userId: user?.id ?? null,
    delivery,
    paymentRef: razorpay_payment_id,
  });

  if ("error" in result) {
    return NextResponse.json(result, { status: 500 });
  }
  return NextResponse.json({ ...result, paymentId: razorpay_payment_id });
}
