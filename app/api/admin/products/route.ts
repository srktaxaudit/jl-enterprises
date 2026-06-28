import { NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";
import { createAdminClient } from "@/lib/supabase/admin";

function slugify(s: string) {
  return s.toLowerCase().trim().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
}

async function categoryId(sb: any, slug?: string) {
  if (!slug) return null;
  const { data } = await sb.from("categories").select("id").eq("slug", slug).maybeSingle();
  return data?.id ?? null;
}

/** POST — create a new product. */
export async function POST(req: Request) {
  if (!isAdmin()) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const b = await req.json().catch(() => ({}));
  if (!b.name || !b.price) return NextResponse.json({ error: "name and price required" }, { status: 400 });

  const sb = createAdminClient();
  if (!sb) return NextResponse.json({ ok: true, demo: true, slug: slugify(b.name) });

  const row = {
    slug: b.slug ? slugify(b.slug) : slugify(b.name),
    name: b.name,
    brand: b.brand ?? null,
    category_id: await categoryId(sb, b.categorySlug),
    emoji: b.emoji ?? "📦",
    image_url: b.imageUrl ?? null,
    description: b.description ?? null,
    price: Number(b.price),
    mrp: Number(b.mrp ?? b.price),
    stock: Number(b.stock ?? 0),
    emi_per_month: Number(b.emiPerMonth ?? Math.round(Number(b.price) / 21)),
    is_active: b.isActive ?? true,
    is_featured: b.isFeatured ?? false,
  };
  const { data, error } = await sb.from("products").insert(row).select("id, slug").single();
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ ok: true, id: data.id, slug: data.slug });
}

/** PATCH — update fields of an existing product (also used by toggle/stock). */
export async function PATCH(req: Request) {
  if (!isAdmin()) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const b = await req.json().catch(() => ({}));
  if (!b.id) return NextResponse.json({ error: "id required" }, { status: 400 });

  const sb = createAdminClient();
  if (!sb) return NextResponse.json({ ok: true, demo: true });

  const upd: Record<string, unknown> = { updated_at: new Date().toISOString() };
  if ("isActive" in b) upd.is_active = b.isActive;
  if ("isFeatured" in b) upd.is_featured = b.isFeatured;
  if ("stock" in b) upd.stock = Number(b.stock);
  if ("name" in b) upd.name = b.name;
  if ("brand" in b) upd.brand = b.brand;
  if ("price" in b) upd.price = Number(b.price);
  if ("mrp" in b) upd.mrp = Number(b.mrp);
  if ("emoji" in b) upd.emoji = b.emoji;
  if ("imageUrl" in b) upd.image_url = b.imageUrl;
  if ("description" in b) upd.description = b.description;
  if ("emiPerMonth" in b) upd.emi_per_month = Number(b.emiPerMonth);
  if ("categorySlug" in b) upd.category_id = await categoryId(sb, b.categorySlug);

  const { error } = await sb.from("products").update(upd).eq("id", b.id);
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ ok: true });
}
