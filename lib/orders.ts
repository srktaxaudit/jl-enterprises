import { createAdminClient } from "@/lib/supabase/admin";
import { sendOrderConfirmation } from "@/lib/whatsapp";

export type OrderItem = {
  id: string;
  name: string;
  brand: string;
  price: number;
  qty: number;
};

export type OrderContact = {
  name?: string;
  phone?: string;
  address?: string;
  city?: string;
  pincode?: string;
};

export type CreateOrderInput = {
  items: OrderItem[];
  contact?: OrderContact;
  paymentMode: string; // COD | RAZORPAY | EMI
  paymentStatus?: "PENDING" | "PAID";
  subtotal: number;
  discount: number;
  total: number;
};

/**
 * Shared order creator used by both the COD route and the Razorpay verify
 * route. Persists via the service-role client, decrements stock, and fires
 * the WhatsApp confirmation. Falls back to a demo order number when no DB.
 */
export async function createOrder(input: CreateOrderInput) {
  const {
    items,
    contact = {},
    paymentMode,
    paymentStatus = "PENDING",
    subtotal,
    discount,
    total,
  } = input;

  const orderNo = "JL" + Date.now().toString().slice(-6);
  const admin = createAdminClient();

  // Demo mode (no Supabase) — still confirm + try WhatsApp (no-op without creds)
  if (!admin) {
    await sendOrderConfirmation({ phone: contact.phone, orderNo, total, paymentMode });
    return { orderNo, demo: true };
  }

  const { data: order, error } = await admin
    .from("orders")
    .insert({
      order_no: orderNo,
      is_guest: true,
      contact_name: contact.name ?? null,
      contact_phone: contact.phone ?? null,
      address: contact.address ?? null,
      city: contact.city ?? null,
      pincode: contact.pincode ?? null,
      state: "Tamil Nadu",
      payment_mode: paymentMode,
      payment_status: paymentStatus,
      status: "NEW",
      subtotal,
      discount,
      total,
    })
    .select()
    .single();

  if (error || !order) {
    return { error: error?.message ?? "Could not create order" };
  }

  if (items.length) {
    await admin.from("order_items").insert(
      items.map((i) => ({
        order_id: order.id,
        product_id: i.id,
        name: i.name,
        brand: i.brand,
        price: i.price,
        qty: i.qty,
      }))
    );
    for (const i of items) {
      await admin.rpc("decrement_stock", { p_id: i.id, p_qty: i.qty }).then(
        () => {},
        () => {}
      );
    }
  }

  await sendOrderConfirmation({ phone: contact.phone, orderNo, total, paymentMode });
  return { orderNo, id: order.id };
}
