import { inr } from "./format";

// ════════════════════════════════════════════════════════════════════
//  Email helper (Resend REST API — no SDK dependency), mirroring the
//  WhatsApp helper: no-ops with a console log until RESEND_API_KEY is
//  set, so order flows never break during development.
// ════════════════════════════════════════════════════════════════════

function creds() {
  const key = process.env.RESEND_API_KEY;
  if (!key) return null;
  return {
    key,
    from: process.env.EMAIL_FROM || "JL Enterprises <onboarding@resend.dev>",
  };
}

export const isEmailConfigured = () => Boolean(creds());

export async function sendEmail(to: string, subject: string, html: string) {
  const c = creds();
  if (!c || !to) {
    console.log(`[email:demo] → ${to}: ${subject}`);
    return { sent: false, demo: true };
  }
  try {
    const res = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: { Authorization: `Bearer ${c.key}`, "Content-Type": "application/json" },
      body: JSON.stringify({ from: c.from, to: [to], subject, html }),
    });
    const data = await res.json().catch(() => ({}));
    return { sent: res.ok, data };
  } catch (e) {
    return { sent: false, error: String(e) };
  }
}

const wrap = (body: string) => `
  <div style="font-family:Segoe UI,Arial,sans-serif;max-width:560px;margin:0 auto;color:#334155">
    <div style="background:#0b2447;color:#fff;padding:16px 20px;border-radius:12px 12px 0 0">
      <b>JL ENTERPRISES</b><div style="font-size:11px;color:#9db4d0">HOME APPLIANCES · THOOTHUKUDI</div>
    </div>
    <div style="border:1px solid #e2e8f0;border-top:0;padding:20px;border-radius:0 0 12px 12px">${body}</div>
    <p style="font-size:11px;color:#94a3b8;text-align:center;margin-top:10px">
      JL Enterprises, 185G/1B, Palai Road, Chidambaramnagar, Thoothukudi 628008
    </p>
  </div>`;

export async function sendOrderEmailConfirmation(opts: {
  email?: string | null;
  name?: string | null;
  orderNo: string;
  total: number;
  paymentMode: string;
  deliveryDate?: string | null;
  deliverySlot?: string | null;
}) {
  const { email, name, orderNo, total, paymentMode, deliveryDate, deliverySlot } = opts;
  if (!email) return { sent: false };
  const slot =
    deliveryDate && deliverySlot
      ? `<p>Preferred delivery: <b>${deliveryDate}</b>, ${deliverySlot}</p>`
      : "";
  return sendEmail(
    email,
    `Order #${orderNo} confirmed — JL Enterprises`,
    wrap(
      `<h2 style="color:#0b2447;margin-top:0">Order confirmed 🎉</h2>
       <p>Hi ${name || "there"}, your order <b>#${orderNo}</b> is confirmed.</p>
       <p>Amount: <b>${inr(total)}</b> (${paymentMode})</p>${slot}
       <p>We'll keep you posted on WhatsApp and email as it moves to delivery. Thank you for shopping with us! 🙏</p>`
    )
  );
}

export async function sendOrderEmailStatus(opts: {
  email?: string | null;
  orderNo: string;
  status: string;
}) {
  const { email, orderNo, status } = opts;
  if (!email) return { sent: false };
  const map: Record<string, string> = {
    PACKED: "📦 packed and ready",
    OUT_FOR_DELIVERY: "🚚 out for delivery",
    DELIVERED: "✅ delivered",
    CANCELLED: "❌ cancelled",
  };
  return sendEmail(
    email,
    `Order #${orderNo} update — JL Enterprises`,
    wrap(
      `<h2 style="color:#0b2447;margin-top:0">Order update</h2>
       <p>Your order <b>#${orderNo}</b> is now <b>${map[status] ?? status}</b>.</p>
       <p>Questions? Just reply to this email or call us. 🙏</p>`
    )
  );
}

export async function sendReturnEmailStatus(opts: {
  email?: string | null;
  orderNo: string;
  kind: string;
  status: string;
}) {
  const { email, orderNo, kind, status } = opts;
  if (!email) return { sent: false };
  const label = kind === "CANCEL" ? "cancellation" : "return";
  const map: Record<string, string> = {
    REQUESTED: "received — we'll confirm within 1 working day",
    APPROVED: "approved — our team will contact you for pickup",
    REJECTED: "not approved — we'll call you to explain",
    PICKED_UP: "picked up — refund is being processed",
    REFUNDED: "refunded — amount will reflect in 5–7 business days",
  };
  return sendEmail(
    email,
    `Return update for order #${orderNo} — JL Enterprises`,
    wrap(
      `<h2 style="color:#0b2447;margin-top:0">Your ${label} request</h2>
       <p>Order <b>#${orderNo}</b>: your ${label} request is <b>${map[status] ?? status}</b>.</p>`
    )
  );
}
