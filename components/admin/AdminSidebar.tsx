"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LogoMark } from "@/components/Logo";

const NAV = [
  { group: "Overview", items: [
    { href: "/admin", label: "Dashboard", emoji: "📊" },
    { href: "/admin/orders", label: "Orders", emoji: "📥" },
  ]},
  { group: "Catalog & Sales", items: [
    { href: "/admin/products", label: "Products", emoji: "📦" },
    { href: "/admin/inventory", label: "Inventory", emoji: "🗂️" },
    { href: "/admin/offers", label: "Offers & Deals", emoji: "🏷️" },
  ]},
  { group: "Engage", items: [
    { href: "/admin/customers", label: "Customers (CRM)", emoji: "👥" },
    { href: "/admin/whatsapp", label: "WhatsApp Offers", emoji: "💬" },
  ]},
  { group: "Control", items: [
    { href: "/admin/settings", label: "Teams & Settings", emoji: "⚙️" },
  ]},
];

/** Inner sidebar content — the outer container is provided by AdminShell. */
export default function AdminSidebar({ onNavigate }: { onNavigate?: () => void }) {
  const path = usePathname();
  const router = useRouter();

  async function logout() {
    await fetch("/api/admin/logout", { method: "POST" });
    router.push("/admin/login");
    router.refresh();
  }

  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center gap-2.5 px-5 pb-4 border-b border-white/10 mb-3">
        <LogoMark className="w-11 h-9" />
        <div>
          <div className="font-bold text-[15px] text-white">JL ENTERPRISES</div>
          <div className="text-[10px] text-brand-sky tracking-wider">CEO CONTROL CENTRE</div>
        </div>
      </div>

      <div className="flex-1 overflow-auto">
        {NAV.map((g) => (
          <div key={g.group}>
            <div className="text-[11px] tracking-widest text-slate-400 uppercase px-5 pt-3 pb-1.5">{g.group}</div>
            {g.items.map((it) => {
              const on = path === it.href;
              return (
                <Link
                  key={it.href}
                  href={it.href}
                  onClick={onNavigate}
                  className={`flex items-center gap-3 px-5 py-2.5 text-sm border-l-[3px] ${
                    on ? "bg-orange/20 border-orange text-white font-semibold" : "border-transparent text-slate-300 hover:bg-white/5 hover:text-white"
                  }`}
                >
                  <span className="w-5 text-center">{it.emoji}</span> {it.label}
                </Link>
              );
            })}
          </div>
        ))}
      </div>

      <div className="px-5 pt-3 border-t border-white/10">
        <Link href="/" onClick={onNavigate} className="block text-slate-300 hover:text-white text-[13px] py-1.5">↗ View store</Link>
        <button onClick={logout} className="block text-slate-300 hover:text-white text-[13px] py-1.5">⎋ Logout</button>
      </div>
    </div>
  );
}
