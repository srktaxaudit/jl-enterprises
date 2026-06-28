// ════════════════════════════════════════════════════════════════════
//  WhatsApp Business Cloud API (Meta) helper.
//  No-ops (returns {sent:false}) until JL's WHATSAPP_TOKEN + PHONE_ID set,
//  so order flows never break during development.
//  NOTE: business-initiated messages (order updates) require an approved
//  template in production. Free-form text only works inside the 24h window.
// ════════════════════════════════════════════════════════════════════

const GRAPH = "https://graph.facebook.com/v20.0";

/** Normalise an Indian mobile number to E.164 digits (e.g. 919876543210). */
export function normalisePhone(raw?: string | null): string | null {
  if (!raw) return null;
  const digits = raw.replace(/\D/g, "");
  if (digits.length === 10) return "91" + digits;
  if (digits.length === 12 && digits.startsWith("91")) return digits;
  if (digits.length === 11 && digits.startsWith("0")) return "91" + digits.slice(1);
  return digits || null;
}

function creds() {
  const token = process.env.WHATSAPP_TOKEN;
  const phoneId = process.env.WHATSAPP_PHONE_ID;
  if (!token || !phoneId) return null;
  return { token, phoneId };
}

export const isWhatsAppConfigured = () => Boolean(creds());

/** Send a plain text message (works within the 24h customer window). */
export async function sendText(to: string, body: string) {
  const c = creds();
  const phone = normalisePhone(to);
  if (!c || !phone) {
    console.log(`[whatsapp:demo] → ${to}: ${body}`);
    return { sent: false, demo: true };
  }
  try {
    const res = await fetch(`${GRAPH}/${c.phoneId}/messages`, {
      method: "POST",
      headers: { Authorization: `Bearer ${c.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        messaging_product: "whatsapp",
        to: phone,
        type: "text",
        text: { body },
      }),
    });
    const data = await res.json();
    return { sent: res.ok, data };
  } catch (e) {
    return { sent: false, error: String(e) };
  }
}

/** Send an approved template message (for business-initiated order updates). */
export async function sendTemplate(
  to: string,
  template: string,
  params: string[] = [],
  lang = "en"
) {
  const c = creds();
  const phone = normalisePhone(to);
  if (!c || !phone) {
    console.log(`[whatsapp:demo] template ${template} → ${to} [${params.join(", ")}]`);
    return { sent: false, demo: true };
  }
  try {
    const res = await fetch(`${GRAPH}/${c.phoneId}/messages`, {
      method: "POST",
      headers: { Authorization: `Bearer ${c.token}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        messaging_product: "whatsapp",
        to: phone,
        type: "template",
        template: {
          name: template,
          language: { code: lang },
          components: params.length
            ? [{ type: "body", parameters: params.map((t) => ({ type: "text", text: t })) }]
            : [],
        },
      }),
    });
    const data = await res.json();
    return { sent: res.ok, data };
  } catch (e) {
    return { sent: false, error: String(e) };
  }
}

/** Order status update (Packed / Out for delivery / Delivered). */
export async function sendOrderStatus(opts: {
  phone?: string | null;
  orderNo: string;
  status: string;
}) {
  const { phone, orderNo, status } = opts;
  if (!phone) return { sent: false };
  const map: Record<string, string> = {
    PACKED: "📦 packed and ready",
    OUT_FOR_DELIVERY: "🚚 out for delivery",
    DELIVERED: "✅ delivered",
    CANCELLED: "❌ cancelled",
  };
  const human = map[status] ?? status;
  return sendText(
    phone,
    `JL Enterprises — your order *#${orderNo}* is now ${human}. Thank you! 🙏`
  );
}

/** Order confirmation — uses template if WHATSAPP_TPL_ORDER is set, else text. */
export async function sendOrderConfirmation(opts: {
  phone?: string | null;
  orderNo: string;
  total: number;
  paymentMode: string;
}) {
  const { phone, orderNo, total, paymentMode } = opts;
  if (!phone) return { sent: false };
  const amount = "₹" + Math.round(total).toLocaleString("en-IN");
  const tpl = process.env.WHATSAPP_TPL_ORDER;
  if (tpl) {
    return sendTemplate(phone, tpl, [orderNo, amount, paymentMode]);
  }
  return sendText(
    phone,
    `🛍️ JL Enterprises\nYour order *#${orderNo}* is confirmed!\nAmount: ${amount} (${paymentMode}).\nWe'll send delivery updates here. Thank you for shopping with us! 🙏`
  );
}
