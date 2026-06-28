import { NextResponse } from "next/server";
import { createAdminClient } from "@/lib/supabase/admin";
import { isAdmin } from "@/lib/admin-auth";
import { sendText, isWhatsAppConfigured } from "@/lib/whatsapp";

/**
 * POST /api/whatsapp/broadcast — bulk offer blast to customers.
 * Allowed for a logged-in admin (session cookie) OR a valid x-admin-token.
 *
 * Body: { message: string, audience?: "all" | "ac" | "repeat" }
 */
export async function POST(req: Request) {
  const token = req.headers.get("x-admin-token");
  const expected = process.env.ADMIN_API_TOKEN;
  const tokenOk = expected ? token === expected : false;
  if (!isAdmin() && !tokenOk) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const { message, audience = "all" } = await req.json().catch(() => ({}));
  if (!message) {
    return NextResponse.json({ error: "message required" }, { status: 400 });
  }

  const admin = createAdminClient();

  // No DB → demo: report what would have been sent.
  if (!admin) {
    return NextResponse.json({
      queued: 0,
      demo: true,
      whatsappConfigured: isWhatsAppConfigured(),
      note: "Connect Supabase + WhatsApp to send to real customers.",
    });
  }

  let query = admin.from("customers").select("phone").not("phone", "is", null);
  // (audience filters like 'ac buyers' would join orders — simplified for now)
  const { data, error } = await query.limit(1000);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });

  const phones = (data ?? []).map((r) => r.phone).filter(Boolean) as string[];
  let sent = 0;
  for (const phone of phones) {
    const r = await sendText(phone, message);
    if (r.sent) sent++;
  }

  return NextResponse.json({ audience, recipients: phones.length, sent });
}
