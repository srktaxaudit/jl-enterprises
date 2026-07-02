import { createAdminClient } from "@/lib/supabase/admin";
import { sendOrderConfirmation } from "@/lib/whatsapp";
import { sendOrderEmailConfirmation } from "@/lib/email";

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
  email?: string | null; // for order-update emails
  userId?: string | null; // auth.users id when the customer is logged in
  delivery?: { date: string; slot: string } | null;
  paymentRef?: string | null; // razorpay payment id
};

/**
 * Shared order creator used by both the COD route and the Razorpay verify
 * route. Persists via the service-role client, decrements stock, and fires
 * the WhatsApp + email confirmations. Falls back to a demo order number
 * when no DB.
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
    email = null,
    userId = null,
    delivery = null,
    paymentRef = null,
  } = input;

  const orderNo = "JL" + Date.now().toString().slice(-6);
  const admin = createAdminClient();

  const notify = async () => {
    await sendOrderConfirmation({ phone: contact.phone, orderNo, total, paymentMode });
    await sendOrderEmailConfirmation({
      email,
      name: contact.name,
      orderNo,
      total,
      paymentMode,
      deliveryDate: delivery?.date,
      deliverySlot: delivery?.slot,
    });
  };

  // Demo mode (no Supabase) — still confirm + try notifications (no-op without creds)
  if (!admin) {
    await notify();
    return { orderNo, demo: true };
  }

  const baseRow = {
    order_no: orderNo,
    is_guest: !userId,
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
  };
  // Columns added by schema-v2.sql — retried without them if the
  // migration hasn't been run on this database yet.
  const v2Row = {
    ...baseRow,
    email,
    user_id: userId,
    delivery_date: delivery?.date ?? null,
    delivery_slot: delivery?.slot ?? null,
    payment_ref: paymentRef,
  };

  let { data: order, error } = await admin
    .from("orders")
    .insert(v2Row)
    .select()
    .single();

  if (error && /column|schema cache/i.test(error.message)) {
    ({ data: order, error } = await admin.from("orders").insert(baseRow).select().single());
  }

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

  await notify();
  return { orderNo, id: order.id };
}
