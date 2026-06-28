import { NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";
import { createAdminClient } from "@/lib/supabase/admin";
import { sendOrderStatus } from "@/lib/whatsapp";

/** POST /api/admin/orders — update an order's status (and notify on WhatsApp). */
export async function POST(req: Request) {
  if (!isAdmin()) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id, status, orderNo, phone } = await req.json().catch(() => ({}));

  const sb = createAdminClient();
  if (!sb) {
    // Demo mode — still fire the WhatsApp helper (logs in demo).
    await sendOrderStatus({ phone, orderNo, status });
    return NextResponse.json({ ok: true, demo: true });
  }

  const { data, error } = await sb
    .from("orders")
    .update({ status })
    .eq("id", id)
    .select("order_no, contact_phone")
    .single();
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });

  await sendOrderStatus({
    phone: data?.contact_phone,
    orderNo: data?.order_no ?? orderNo,
    status,
  });
  return NextResponse.json({ ok: true });
}
