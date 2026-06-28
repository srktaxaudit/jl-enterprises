import { adminCustomers } from "@/lib/admin-data";
import { inr } from "@/lib/format";
import { PageHead } from "@/components/admin/ui";

export default async function CustomersPage() {
  const customers = await adminCustomers();
  return (
    <div>
      <PageHead title="👥 Customers (CRM)" sub="Customer database, order history and follow-ups." />
      <div className="bg-white border border-slate-200 rounded-2xl p-5">
        <table className="w-full text-sm">
          <thead><tr className="text-left text-slate-400 text-[12px] uppercase">
            <th className="py-2">Customer</th><th>Phone</th><th>Area</th><th>Orders</th><th className="text-right">Lifetime Value</th>
          </tr></thead>
          <tbody>
            {customers.map((c) => (
              <tr key={c.id} className="border-t border-slate-100">
                <td className="py-2.5 font-semibold text-navy">{c.name}</td>
                <td className="text-slate-600">{c.phone}</td>
                <td className="text-slate-600">{c.area}</td>
                <td className="text-slate-600">{c.orders}</td>
                <td className="text-right font-bold text-navy">{c.ltv ? inr(c.ltv) : "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
