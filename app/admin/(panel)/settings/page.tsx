import { PageHead } from "@/components/admin/ui";
import { CONTACT_LIST, telHref, prettyPhone } from "@/lib/contact";

const TEAMS = ["Marketing", "Sales", "Accounts", "Services", "CRM"];
const MODULES = [
  ["Orders", [0, 1, 1, 0, 1]],
  ["Deliveries", [0, 1, 0, 1, 0]],
  ["Service Bookings", [0, 0, 0, 1, 1]],
  ["Products", [1, 1, 0, 0, 0]],
  ["Offers & Deals", [1, 0, 0, 0, 0]],
  ["Inventory", [0, 0, 1, 1, 0]],
  ["Accounts", [0, 0, 1, 0, 0]],
  ["Customers (CRM)", [1, 1, 0, 0, 1]],
  ["WhatsApp Offers", [1, 0, 0, 0, 1]],
] as const;

const STAFF = [
  ["Selvi M.", "Marketing", "Team Member"],
  ["Arjun K.", "Sales", "Team Lead"],
  ["Latha R.", "Accounts", "Team Member"],
  ["Anbu S.", "Services", "Technician Lead"],
  ["Divya P.", "CRM", "Team Member"],
];

export default function SettingsPage() {
  return (
    <div>
      <PageHead title="⚙️ Teams & Settings" sub="You (CEO) decide what each team can access — 2 staff logins per team." />

      <div className="bg-white border border-slate-200 rounded-2xl p-5 mb-5">
        <h3 className="text-navy font-semibold mb-4">Permission Matrix</h3>
        <div className="overflow-x-auto"><table className="w-full text-sm min-w-[520px]">
          <thead><tr className="text-slate-400 text-[12px] uppercase">
            <th className="text-left py-2">Module</th>
            {TEAMS.map((t) => <th key={t} className="text-center">{t}</th>)}
          </tr></thead>
          <tbody>
            {MODULES.map(([m, perms]) => (
              <tr key={m} className="border-t border-slate-100">
                <td className="py-2.5 font-semibold text-navy text-left">{m}</td>
                {perms.map((v, i) => <td key={i} className="text-center text-lg">{v ? "✅" : "⬜"}</td>)}
              </tr>
            ))}
          </tbody>
        </table></div>
      </div>

      <div className="grid md:grid-cols-2 gap-5">
        <div className="bg-white border border-slate-200 rounded-2xl p-5">
          <h3 className="text-navy font-semibold mb-4">Team Members</h3>
          <div className="overflow-x-auto"><table className="w-full text-sm min-w-[520px]">
            <thead><tr className="text-left text-slate-400 text-[12px] uppercase"><th className="py-2">Name</th><th>Team</th><th>Role</th></tr></thead>
            <tbody>
              {STAFF.map(([n, t, r]) => (
                <tr key={n} className="border-t border-slate-100">
                  <td className="py-2.5 font-semibold text-navy">{n}</td>
                  <td className="text-slate-600">{t}</td>
                  <td className="text-slate-600">{r}</td>
                </tr>
              ))}
            </tbody>
          </table></div>
        </div>
        <div className="bg-white border border-slate-200 rounded-2xl p-5">
          <h3 className="text-navy font-semibold mb-4">📞 Team Contacts</h3>
          {CONTACT_LIST.map((c) => (
            <a key={c.phone} href={telHref(c.phone)} className="flex justify-between items-center py-2.5 border-t border-slate-100 first:border-0">
              <span className="text-navy font-semibold text-[14px]">{c.label}</span>
              <span className="text-brand font-bold">{prettyPhone(c.phone)}</span>
            </a>
          ))}
        </div>

        <div className="bg-white border border-slate-200 rounded-2xl p-5">
          <h3 className="text-navy font-semibold mb-4">Integrations</h3>
          {[
            ["💳 Razorpay", "Payments — settles to JL's bank"],
            ["💬 WhatsApp Business", "Meta Cloud API"],
            ["🧾 GST e-Invoice", "Ready to switch on later"],
            ["🌐 Domain", "jlenterprises.in"],
          ].map(([t, s]) => (
            <div key={t} className="flex justify-between items-center py-3 border-t border-slate-100 first:border-0">
              <div><b className="text-navy text-[14px] block">{t}</b><span className="text-[12px] text-slate-400">{s}</span></div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
