import crypto from "crypto";

// ════════════════════════════════════════════════════════════════════
//  Razorpay helper — uses the REST API directly (no SDK dependency).
//  Returns null/false gracefully until JL's keys are set.
// ════════════════════════════════════════════════════════════════════

function keys() {
  const id = process.env.RAZORPAY_KEY_ID;
  const secret = process.env.RAZORPAY_KEY_SECRET;
  if (!id || !secret) return null;
  return { id, secret };
}

export const isRazorpayConfigured = () => Boolean(keys());

/** Create a Razorpay order. Amount is in rupees; converted to paise. */
export async function createRazorpayOrder(amountRupees: number, receipt: string) {
  const k = keys();
  if (!k) return null;

  const auth = Buffer.from(`${k.id}:${k.secret}`).toString("base64");
  const res = await fetch("https://api.razorpay.com/v1/orders", {
    method: "POST",
    headers: { Authorization: `Basic ${auth}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      amount: Math.round(amountRupees * 100), // paise
      currency: "INR",
      receipt,
      payment_capture: 1,
    }),
  });
  if (!res.ok) {
    const err = await res.text();
    throw new Error("Razorpay order failed: " + err);
  }
  const order = await res.json();
  return { orderId: order.id as string, amount: order.amount as number, keyId: k.id };
}

/** Fetch a Razorpay order so its amount can be cross-checked against the
 *  server-priced total before the order is persisted as PAID. */
export async function fetchRazorpayOrder(orderId: string) {
  const k = keys();
  if (!k || !/^order_[A-Za-z0-9]+$/.test(orderId)) return null;

  const auth = Buffer.from(`${k.id}:${k.secret}`).toString("base64");
  const res = await fetch(`https://api.razorpay.com/v1/orders/${orderId}`, {
    headers: { Authorization: `Basic ${auth}` },
  });
  if (!res.ok) return null;
  const order = await res.json();
  return { amountPaise: Number(order.amount), status: String(order.status) };
}

/** Refund a captured payment (full refund when amountPaise is omitted).
 *  Used when the admin marks a return request as REFUNDED. */
export async function refundRazorpayPayment(paymentId: string, amountPaise?: number) {
  const k = keys();
  if (!k) return { ok: false as const, error: "Razorpay keys not configured" };
  if (!/^pay_[A-Za-z0-9]+$/.test(paymentId))
    return { ok: false as const, error: "Invalid payment reference" };

  const auth = Buffer.from(`${k.id}:${k.secret}`).toString("base64");
  try {
    const res = await fetch(`https://api.razorpay.com/v1/payments/${paymentId}/refund`, {
      method: "POST",
      headers: { Authorization: `Basic ${auth}`, "Content-Type": "application/json" },
      body: JSON.stringify(amountPaise ? { amount: Math.round(amountPaise) } : {}),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      return { ok: false as const, error: data?.error?.description ?? "Refund failed" };
    }
    return { ok: true as const, refundId: data.id as string };
  } catch (e) {
    return { ok: false as const, error: String(e) };
  }
}

/** Verify the payment signature returned by Razorpay Checkout. */
export function verifyRazorpaySignature(p: {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
}) {
  const k = keys();
  if (!k) return false;
  const expected = crypto
    .createHmac("sha256", k.secret)
    .update(`${p.razorpay_order_id}|${p.razorpay_payment_id}`)
    .digest("hex");
  // timing-safe compare
  const a = Buffer.from(expected);
  const b = Buffer.from(p.razorpay_signature || "");
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}
