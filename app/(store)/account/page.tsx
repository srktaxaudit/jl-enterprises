import Link from "next/link";
import { redirect } from "next/navigation";
import { getSessionUser, createClient } from "@/lib/supabase/server";
import { createAdminClient } from "@/lib/supabase/admin";
import { inr } from "@/lib/format";
import { STATUS_LABEL } from "@/lib/admin-demo";
import SignOutButton from "@/components/SignOutButton";

export const metadata = { title: "My Account — JL Enterprises" };

const STATUS_CLASS: Record<string, string> = {
  NEW: "bg-pink-100 text-pink-700",
  PACKED: "bg-amber-100 text-amber-700",
  OUT_FOR_DELIVERY: "bg-blue-100 text-blue-700",
  DELIVERED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-600",
};

type AccountOrder = {
  id: string;
  order_no: string;
  status: string;
  payment_mode: string;
  total: number;
  created_at: string;
  order_items: { name: string; qty: number }[] | null;
};

async function fetchMyOrders(userId: string, email: string | undefined): Promise<AccountOrder[]> {
  const admin = createAdminClient();
  if (!admin) return [];
  // user_id/email columns arrive with schema-v2 — fall back to email-only,
  // then to none, so this page never breaks on an un-migrated database.
  const filters = [
    `user_id.eq.${userId}` + (email ? `,email.eq.${email}` : ""),
    email ? `email.eq.${email}` : "",
  ].filter(Boolean);
  for (const f of filters) {
    const { data, error } = await admin
      .from("orders")
      .select("id, order_no, status, payment_mode, total, created_at, order_items(name, qty)")
      .or(f)
      .order("created_at", { ascending: false })
      .limit(50);
    if (!error && data) return data as unknown as AccountOrder[];
  }
  return [];
}

export default async function AccountPage() {
  const sb = createClient();
  if (!sb) {
    return (
      <div className="max-w-[720px] mx-auto px-5 text-center py-20 text-slate-400 animate-fade">
        <span className="text-5xl block mb-3">👤</span>
        Accounts activate once the store database is connected.
        <div className="mt-4">
          <Link href="/track" className="bg-navy text-white px-6 py-2.5 rounded-full font-semibold">
            Track an Order Instead
          </Link>
        </div>
      </div>
    );
  }

  const user = await getSessionUser();
  if (!user) redirect("/login");

  const orders = await fetchMyOrders(user.id, user.email ?? undefined);
  const name = (user.user_metadata as any)?.name as string | undefined;

  return (
    <div className="max-w-[820px] mx-auto px-5 animate-fade">
      <div className="flex items-center justify-between flex-wrap gap-3 mt-6 mb-5">
        <div>
          <h2 className="text-2xl text-navy font-bold">👤 My Account</h2>
          <p className="text-slate-400 text-sm">{name ? `${name} · ` : ""}{user.email}</p>
        </div>
        <SignOutButton />
      </div>

      <h3 className="text-navy font-semibold mb-3">My Orders</h3>
      {orders.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-2xl p-8 text-center text-slate-400 mb-8">
          No orders on this account yet.
          <span className="block text-[13px] mt-1">
            Orders placed with your email {user.email} while logged in will appear here.
          </span>
          <div className="mt-4 flex gap-3 justify-center flex-wrap">
            <Link href="/category" className="bg-navy text-white px-5 py-2.5 rounded-full font-semibold text-sm">
              Start Shopping
            </Link>
            <Link href="/track" className="border border-slate-200 text-slate-600 px-5 py-2.5 rounded-full font-semibold text-sm">
              Track a Guest Order
            </Link>
          </div>
        </div>
      ) : (
        <div className="grid gap-3 mb-8">
          {orders.map((o) => (
            <div key={o.id} className="bg-white border border-slate-200 rounded-2xl p-4">
              <div className="flex items-center justify-between flex-wrap gap-2 mb-2">
                <div className="font-bold text-navy">#{o.order_no}</div>
                <span className={`text-[11px] font-bold px-2.5 py-1 rounded-full ${STATUS_CLASS[o.status] ?? "bg-slate-100 text-slate-600"}`}>
                  {STATUS_LABEL[o.status] ?? o.status}
                </span>
              </div>
              <div className="text-[13px] text-slate-500 mb-1.5">
                {new Date(o.created_at).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}
                {" · "}{o.payment_mode}
              </div>
              <div className="text-[13px] text-slate-600">
                {(o.order_items ?? []).map((i) => `${i.name} ×${i.qty}`).join(", ") || "—"}
              </div>
              <div className="flex items-center justify-between mt-2 pt-2 border-t border-slate-100">
                <b className="text-navy">{inr(Number(o.total))}</b>
                <Link href="/track" className="text-brand text-[13px] font-semibold">Track →</Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
