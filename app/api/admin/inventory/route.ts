import { NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";
import { createAdminClient } from "@/lib/supabase/admin";

/** POST /api/admin/inventory — set a product's stock level. */
export async function POST(req: Request) {
  if (!isAdmin()) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { id, stock } = await req.json().catch(() => ({}));

  const sb = createAdminClient();
  if (!sb) return NextResponse.json({ ok: true, demo: true });

  const { error } = await sb.from("products").update({ stock: Number(stock), updated_at: new Date().toISOString() }).eq("id", id);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ ok: true });
}
