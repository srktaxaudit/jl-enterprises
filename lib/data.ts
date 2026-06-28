import { createClient } from "@/lib/supabase/server";
import { CATEGORIES, PRODUCTS } from "./catalog";
import type { Category, Product } from "./types";

// ════════════════════════════════════════════════════════════════════
//  Server data layer. Reads from Supabase when configured, otherwise
//  falls back to the seed catalog so the store always renders.
//  (Used only by Server Components — imports next/headers via server.ts)
// ════════════════════════════════════════════════════════════════════

function rowToProduct(r: any): Product {
  return {
    id: r.id,
    slug: r.slug,
    name: r.name,
    brand: r.brand ?? "",
    categorySlug: r.category?.slug ?? "",
    emoji: r.emoji ?? "📦",
    imageUrl: r.image_url ?? null,
    description: r.description ?? "",
    specs: r.specs ?? {},
    price: Number(r.price),
    mrp: Number(r.mrp ?? r.price),
    stock: Number(r.stock ?? 0),
    rating: Number(r.rating ?? 4.5),
    reviewCount: Number(r.review_count ?? 0),
    emiPerMonth: Number(r.emi_per_month ?? Math.round(Number(r.price) / 21)),
    isActive: r.is_active ?? true,
    isFeatured: r.is_featured ?? false,
  };
}

const PRODUCT_SELECT = "*, category:categories(slug,name)";

export async function fetchCategories(): Promise<Category[]> {
  const sb = createClient();
  if (!sb) return CATEGORIES;
  const { data, error } = await sb
    .from("categories")
    .select("*")
    .eq("is_active", true)
    .order("sort_order");
  if (error || !data?.length) return CATEGORIES;
  return data.map((c) => ({ id: c.id, slug: c.slug, name: c.name, emoji: c.emoji ?? "📦" }));
}

export async function fetchProducts(categorySlug?: string): Promise<Product[]> {
  const sb = createClient();
  if (!sb) {
    const active = PRODUCTS.filter((p) => p.isActive);
    return categorySlug ? active.filter((p) => p.categorySlug === categorySlug) : active;
  }
  const { data, error } = await sb
    .from("products")
    .select(PRODUCT_SELECT)
    .eq("is_active", true)
    .order("created_at");
  if (error || !data) return PRODUCTS.filter((p) => p.isActive);
  let list = data.map(rowToProduct);
  if (categorySlug) list = list.filter((p) => p.categorySlug === categorySlug);
  return list;
}

export async function fetchFeatured(): Promise<Product[]> {
  const sb = createClient();
  if (!sb) return PRODUCTS.filter((p) => p.isActive && p.isFeatured);
  const { data, error } = await sb
    .from("products")
    .select(PRODUCT_SELECT)
    .eq("is_active", true)
    .eq("is_featured", true);
  if (error || !data?.length) return PRODUCTS.filter((p) => p.isActive && p.isFeatured);
  return data.map(rowToProduct);
}

export async function fetchProductBySlug(slug: string): Promise<Product | undefined> {
  const sb = createClient();
  if (!sb) return PRODUCTS.find((p) => p.slug === slug && p.isActive);
  const { data, error } = await sb
    .from("products")
    .select(PRODUCT_SELECT)
    .eq("slug", slug)
    .eq("is_active", true)
    .maybeSingle();
  if (error || !data) return PRODUCTS.find((p) => p.slug === slug && p.isActive);
  return rowToProduct(data);
}

export async function fetchCategoryBySlug(slug: string): Promise<Category | undefined> {
  const cats = await fetchCategories();
  return cats.find((c) => c.slug === slug);
}
