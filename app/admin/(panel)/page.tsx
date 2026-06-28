import Link from "next/link";
import { adminStats, adminOrders, adminProducts } from "@/lib/admin-data";
import { inr } from "@/lib/format";
import { StatCard, StatusBadge } from "@/components/admin/ui";

export default async function AdminDashboard() {
  const [stats, orders, products] = await Promise.all([
    adminStats(), adminOrders(), adminProducts(),
  ]);
  const low = products.filter((p) => p.stock <= 3);

  return (
    <div>
      <div className="bg-orange-50 border border-dashed border-orange-300 text-orange-800 text-[13px] text-center py-2 rounded-xl mb-5">
        👑 CEO Super-Admin — full visibility across every team. {orders === undefined ? "" : ""}Working panel on {stats.orders ? "live/demo" : ""} data.
      </div>
      <h2 className="text-2xl text-navy font-bold mb-1">Good morning, Boss 👋</h2>
      <p className="text-slate-400 text-sm mb-5">Here's what's happening at JL Enterprises today.</p>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
        <StatCard label="Sales" value={inr(stats.sales)} sub="recent orders" />
        <StatCard label="Orders" value={String(stats.orders)} sub="in system" />
        <StatCard label="Pending" value={String(stats.pending)} sub="to fulfil" />
        <StatCard label="Low Stock" value={String(stats.lowStock)} sub="need restock" />
        <StatCard label="Customers" value={String(stats.customers)} sub="total" />
      </div>

      <div className="grid md:grid-cols-[1.6fr_1fr] gap-5">
        <div className="bg-white border border-slate-200 rounded-2xl p-5">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-navy font-semibold">Recent Orders</h3>
            <Link href="/admin/orders" className="text-brand text-[13px]">All orders →</Link>
          </div>
          <table className="w-full text-sm">
            <thead><tr className="text-left text-slate-400 text-[12px] uppercase">
              <th className="py-2">Order</th><th>Customer</th><th>Status</th><th className="text-right">Amount</th>
            </tr></thead>
            <tbody>
              {orders.slice(0, 6).map((o) => (
                <tr key={o.id} className="border-t border-slate-100">
                  <td className="py-2.5 font-bold text-navy">#{o.order_no}</td>
                  <td className="text-slate-600">{o.contact_name}</td>
                  <td><StatusBadge status={o.status} /></td>
                  <td className="text-right font-bold text-navy">{inr(o.total)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="bg-white border border-slate-200 rounded-2xl p-5">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-navy font-semibold">⚠️ Low Stock</h3>
            <Link href="/admin/inventory" className="text-brand text-[13px]">Inventory →</Link>
          </div>
          {low.length === 0 && <p className="text-slate-400 text-sm">All healthy 🎉</p>}
          {low.map((p) => (
            <div key={p.id} className="flex items-center gap-3 py-2.5 border-t border-slate-100 first:border-0">
              <span className="text-xl">{p.emoji}</span>
              <div className="flex-1">
                <b className="text-navy text-[13px] block">{p.name}</b>
                <span className="text-[12px] text-slate-400">{p.brand}</span>
              </div>
              <b className="text-red-600">{p.stock} left</b>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
