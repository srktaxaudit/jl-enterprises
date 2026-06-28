import { createAdminClient } from "@/lib/supabase/admin";
import { PRODUCTS } from "@/lib/catalog";
import { DEMO_ORDERS, DEMO_CUSTOMERS, type AdminOrder, type AdminCustomer } from "@/lib/admin-demo";
import type { Product } from "@/lib/types";

// All admin reads go through the service-role client (sees inactive rows too).
// Everything falls back to demo/seed data when Supabase isn't configured.

export async function adminProducts(): Promise<Product[]> {
  const sb = createAdminClient();
  if (!sb) return PRODUCTS;
  const { data, error } = await sb
    .from("products")
    .select("*, category:categories(slug,name)")
    .order("created_at");
  if (error || !data) return PRODUCTS;
  return data.map((r: any) => ({
    id: r.id, slug: r.slug, name: r.name, brand: r.brand ?? "",
    categorySlug: r.category?.slug ?? "", emoji: r.emoji ?? "📦",
    imageUrl: r.image_url, description: r.description ?? "", specs: r.specs ?? {},
    price: Number(r.price), mrp: Number(r.mrp ?? r.price), stock: Number(r.stock ?? 0),
    rating: Number(r.rating ?? 4.5), reviewCount: Number(r.review_count ?? 0),
    emiPerMonth: Number(r.emi_per_month ?? 0), isActive: r.is_active ?? true, isFeatured: r.is_featured ?? false,
  }));
}

export async function adminOrders(): Promise<AdminOrder[]> {
  const sb = createAdminClient();
  if (!sb) return DEMO_ORDERS;
  const { data, error } = await sb
    .from("orders")
    .select("id, order_no, contact_name, city, payment_mode, status, total, order_items(name)")
    .order("created_at", { ascending: false })
    .limit(100);
  if (error || !data?.length) return DEMO_ORDERS;
  return data.map((r: any) => ({
    id: r.id, order_no: r.order_no, contact_name: r.contact_name ?? "Guest",
    city: r.city ?? "—", product: r.order_items?.[0]?.name ?? "—",
    payment_mode: r.payment_mode, status: r.status, total: Number(r.total),
  }));
}

export async function adminCustomers(): Promise<AdminCustomer[]> {
  const sb = createAdminClient();
  if (!sb) return DEMO_CUSTOMERS;
  const { data, error } = await sb.from("customers").select("*").limit(200);
  if (error || !data?.length) return DEMO_CUSTOMERS;
  return data.map((r: any) => ({
    id: r.id, name: r.name ?? "—", phone: r.phone ?? "—",
    area: r.area ?? "—", orders: 0, ltv: 0,
  }));
}

export type Stats = {
  sales: number; orders: number; pending: number; lowStock: number; customers: number;
};

export async function adminStats(): Promise<Stats> {
  const [orders, products, customers] = await Promise.all([
    adminOrders(), adminProducts(), adminCustomers(),
  ]);
  return {
    sales: orders.reduce((s, o) => s + o.total, 0),
    orders: orders.length,
    pending: orders.filter((o) => o.status !== "DELIVERED" && o.status !== "CANCELLED").length,
    lowStock: products.filter((p) => p.stock <= 3).length,
    customers: customers.length,
  };
}
