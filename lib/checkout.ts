// ════════════════════════════════════════════════════════════════════
//  Shared checkout rules — safe to import from both client components
//  (for inline form feedback) and API routes (the authoritative check).
// ════════════════════════════════════════════════════════════════════

/** Flat old-appliance exchange bonus applied to every order (business rule). */
export const EXCHANGE_BONUS = 3000;

export const MAX_QTY_PER_ITEM = 10;
export const MAX_ITEMS_PER_ORDER = 20;

export type CheckoutContact = {
  name: string;
  phone: string;
  address: string;
  city: string;
  pincode: string;
};

export type ContactResult =
  | { ok: true; contact: CheckoutContact }
  | { ok: false; error: string };

/** Validate + normalise the delivery contact. Phone is reduced to the
 *  10-digit Indian mobile (leading +91 / 0 stripped). */
export function validateContact(raw: unknown): ContactResult {
  const r = (raw ?? {}) as Record<string, unknown>;
  const name = String(r.name ?? "").trim();
  const address = String(r.address ?? "").trim();
  const city = String(r.city ?? "").trim();
  const pincode = String(r.pincode ?? "").trim();

  let phone = String(r.phone ?? "").replace(/\D/g, "");
  if (phone.length === 12 && phone.startsWith("91")) phone = phone.slice(2);
  if (phone.length === 11 && phone.startsWith("0")) phone = phone.slice(1);

  if (name.length < 2 || name.length > 80)
    return { ok: false, error: "Please enter your full name." };
  if (!/^[6-9]\d{9}$/.test(phone))
    return { ok: false, error: "Please enter a valid 10-digit mobile number." };
  if (address.length < 10 || address.length > 500)
    return { ok: false, error: "Please enter your complete delivery address (door no, street, area)." };
  if (city.length < 2 || city.length > 60)
    return { ok: false, error: "Please enter your city." };
  if (!/^[1-9]\d{5}$/.test(pincode))
    return { ok: false, error: "Please enter a valid 6-digit pincode." };

  return { ok: true, contact: { name, phone, address, city, pincode } };
}

/** Optional email for order updates — empty is fine, invalid is not. */
export function validateEmail(raw: unknown): { ok: true; email: string | null } | { ok: false; error: string } {
  const email = String(raw ?? "").trim();
  if (!email) return { ok: true, email: null };
  if (email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email))
    return { ok: false, error: "Please enter a valid email address (or leave it blank)." };
  return { ok: true, email: email.toLowerCase() };
}

// ── Delivery slots ───────────────────────────────────────────────────
export const DELIVERY_SLOTS = ["9 AM – 12 PM", "12 PM – 4 PM", "4 PM – 8 PM"];
export const DELIVERY_MIN_DAYS = 1; // earliest: tomorrow
export const DELIVERY_MAX_DAYS = 14;

/** YYYY-MM-DD in local time, `offset` days from today. */
export function deliveryDateOption(offset: number): string {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${m}-${day}`;
}

export type DeliveryResult =
  | { ok: true; date: string; slot: string }
  | { ok: false; error: string };

/** Validate the preferred delivery date (tomorrow … +14 days) and slot. */
export function validateDelivery(raw: unknown): DeliveryResult {
  const r = (raw ?? {}) as Record<string, unknown>;
  const date = String(r.date ?? "").trim();
  const slot = String(r.slot ?? "").trim();

  if (!/^\d{4}-\d{2}-\d{2}$/.test(date))
    return { ok: false, error: "Please pick a delivery date." };
  if (!DELIVERY_SLOTS.includes(slot))
    return { ok: false, error: "Please pick a delivery time slot." };

  // Compare as date-only strings to avoid timezone edge cases.
  const earliest = deliveryDateOption(0); // allow same-day, business confirms
  const latest = deliveryDateOption(DELIVERY_MAX_DAYS);
  if (date < earliest || date > latest)
    return { ok: false, error: "Delivery date must be within the next two weeks." };

  return { ok: true, date, slot };
}

export type CartItemRef = { id: string; qty: number };

export type ItemsResult =
  | { ok: true; items: CartItemRef[] }
  | { ok: false; error: string };

/** Validate the shape of cart items: ids + sane quantities only.
 *  Prices are NEVER accepted from the client — see lib/pricing.ts. */
export function validateItemRefs(raw: unknown): ItemsResult {
  if (!Array.isArray(raw) || raw.length === 0)
    return { ok: false, error: "Your cart is empty." };
  if (raw.length > MAX_ITEMS_PER_ORDER)
    return { ok: false, error: `A single order can have at most ${MAX_ITEMS_PER_ORDER} items.` };

  const byId = new Map<string, number>();
  for (const entry of raw) {
    const id = String((entry as any)?.id ?? "").trim();
    const qty = Number((entry as any)?.qty);
    if (!id || id.length > 64)
      return { ok: false, error: "Invalid item in cart." };
    if (!Number.isInteger(qty) || qty < 1 || qty > MAX_QTY_PER_ITEM)
      return { ok: false, error: `Quantity per item must be between 1 and ${MAX_QTY_PER_ITEM}.` };
    byId.set(id, Math.min((byId.get(id) ?? 0) + qty, MAX_QTY_PER_ITEM));
  }
  return { ok: true, items: Array.from(byId, ([id, qty]) => ({ id, qty })) };
}
