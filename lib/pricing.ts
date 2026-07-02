import { createAdminClient } from "@/lib/supabase/admin";
import { PRODUCTS } from "@/lib/catalog";
import { EXCHANGE_BONUS, validateItemRefs } from "@/lib/checkout";
import type { OrderItem } from "@/lib/orders";

// ════════════════════════════════════════════════════════════════════
//  Server-side order pricing. The client only ever sends product ids
//  and quantities — names, prices and totals are looked up here from
//  the database (or the seed catalog in demo mode) so a tampered
//  request can never change what an order costs.
// ════════════════════════════════════════════════════════════════════

export type PricedOrder = {
  ok: true;
  items: OrderItem[];
  subtotal: number;
  discount: number;
  total: number;
};

export type PricingError = { ok: false; error: string };

type CatalogRow = {
  id: string;
  name: string;
  brand: string | null;
  price: number;
  stock: number;
  is_active: boolean;
};

async function lookupProducts(ids: string[]): Promise<CatalogRow[] | null> {
  const admin = createAdminClient();
  if (!admin) {
    // Demo mode — price from the built-in seed catalog.
    return PRODUCTS.filter((p) => ids.includes(p.id)).map((p) => ({
      id: p.id,
      name: p.name,
      brand: p.brand,
      price: p.price,
      stock: p.stock,
      is_active: p.isActive,
    }));
  }
  const { data, error } = await admin
    .from("products")
    .select("id, name, brand, price, stock, is_active")
    .in("id", ids);
  if (error) return null;
  return (data ?? []).map((r) => ({
    id: r.id,
    name: r.name,
    brand: r.brand,
    price: Number(r.price),
    stock: Number(r.stock ?? 0),
    is_active: r.is_active ?? true,
  }));
}

/** Recompute an order entirely from trusted data. `requireStock` is
 *  relaxed after a payment has already been captured, so a race on
 *  stock never voids a paid order. */
export async function priceOrder(
  rawItems: unknown,
  opts: { requireStock?: boolean } = {}
): Promise<PricedOrder | PricingError> {
  const { requireStock = true } = opts;

  const refs = validateItemRefs(rawItems);
  if (!refs.ok) return refs;

  const rows = await lookupProducts(refs.items.map((i) => i.id));
  if (!rows) return { ok: false, error: "Could not price the order — please try again." };

  const byId = new Map(rows.map((r) => [r.id, r]));
  const items: OrderItem[] = [];
  let subtotal = 0;

  for (const ref of refs.items) {
    const p = byId.get(ref.id);
    if (!p || !p.is_active)
      return { ok: false, error: "An item in your cart is no longer available." };
    if (requireStock && p.stock < ref.qty)
      return { ok: false, error: `Only ${p.stock} unit(s) of "${p.name}" left in stock.` };
    items.push({ id: p.id, name: p.name, brand: p.brand ?? "", price: p.price, qty: ref.qty });
    subtotal += p.price * ref.qty;
  }

  subtotal = Math.round(subtotal * 100) / 100;
  const discount = Math.min(EXCHANGE_BONUS, subtotal);
  const total = Math.round((subtotal - discount) * 100) / 100;

  return { ok: true, items, subtotal, discount, total };
}
