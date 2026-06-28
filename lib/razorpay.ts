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
