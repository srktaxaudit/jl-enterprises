import { PageHead, StatusBadge } from "@/components/admin/ui";

const OFFERS = [
  { title: "Summer AC Sale", type: "Category", value: "Up to 40%", ends: "30 Jun", status: "DELIVERED", live: "Live" },
  { title: "Exchange Bonus", type: "Buyback", value: "+₹3,000", ends: "15 Jul", status: "DELIVERED", live: "Live" },
  { title: "Furniture Fest", type: "Category", value: "22% off", ends: "05 Jul", status: "DELIVERED", live: "Live" },
  { title: "Diwali Dhamaka", type: "Store-wide", value: "TBD", ends: "—", status: "PACKED", live: "Scheduled" },
];

const COUPONS = [
  { code: "JL500", value: "₹500 off ₹10k+", used: 87 },
  { code: "NEWHOME", value: "10% furniture", used: 34 },
  { code: "FESTIVE", value: "₹1,000 off", used: 12 },
];

export default function OffersPage() {
  return (
    <div>
      <PageHead title="🏷️ Offers & Deals" sub="Run discounts, coupons and limited-time deals." />
      <div className="grid md:grid-cols-[1.6fr_1fr] gap-5">
        <div className="bg-white border border-slate-200 rounded-2xl p-5">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-navy font-semibold">Active Offers</h3>
            <button className="bg-navy text-white text-[13px] font-bold px-4 py-2 rounded-lg">+ Create Offer</button>
          </div>
          <table className="w-full text-sm">
            <thead><tr className="text-left text-slate-400 text-[12px] uppercase">
              <th className="py-2">Offer</th><th>Type</th><th>Discount</th><th>Ends</th><th>Status</th>
            </tr></thead>
            <tbody>
              {OFFERS.map((o) => (
                <tr key={o.title} className="border-t border-slate-100">
                  <td className="py-2.5 font-semibold text-navy">{o.title}</td>
                  <td className="text-slate-600">{o.type}</td>
                  <td className="text-slate-600">{o.value}</td>
                  <td className="text-slate-600">{o.ends}</td>
                  <td><StatusBadge status={o.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="bg-white border border-slate-200 rounded-2xl p-5">
          <h3 className="text-navy font-semibold mb-4">Coupon Codes</h3>
          <table className="w-full text-sm">
            <thead><tr className="text-left text-slate-400 text-[12px] uppercase"><th className="py-2">Code</th><th>Value</th><th className="text-right">Used</th></tr></thead>
            <tbody>
              {COUPONS.map((c) => (
                <tr key={c.code} className="border-t border-slate-100">
                  <td className="py-2.5 font-bold text-navy">{c.code}</td>
                  <td className="text-slate-600">{c.value}</td>
                  <td className="text-right text-slate-600">{c.used}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
