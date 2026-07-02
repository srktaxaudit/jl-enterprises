import { NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";
import { createAdminClient } from "@/lib/supabase/admin";
import { sendOrderStatus } from "@/lib/whatsapp";
import { sendOrderEmailStatus } from "@/lib/email";

/** POST /api/admin/orders — update an order's status (and notify the
 *  customer on WhatsApp + email). */
export async function POST(req: Request) {
  if (!isAdmin()) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id, status, orderNo, phone } = await req.json().catch(() => ({}));

  const sb = createAdminClient();
  if (!sb) {
    // Demo mode — still fire the notification helpers (they log in demo).
    await sendOrderStatus({ phone, orderNo, status });
    return NextResponse.json({ ok: true, demo: true });
  }

  // select("*") so this works before and after the schema-v2 email column.
  const { data, error } = await sb
    .from("orders")
    .update({ status })
    .eq("id", id)
    .select("*")
    .single();
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });

  await sendOrderStatus({
    phone: data?.contact_phone,
    orderNo: data?.order_no ?? orderNo,
    status,
  });
  await sendOrderEmailStatus({
    email: data?.email ?? null,
    orderNo: data?.order_no ?? orderNo,
    status,
  });
  return NextResponse.json({ ok: true });
}
