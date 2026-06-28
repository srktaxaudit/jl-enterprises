import Link from "next/link";
import { LogoMark } from "@/components/Logo";
import { CONTACT_LIST, ADDRESS, telHref, prettyPhone } from "@/lib/contact";

export default function Footer() {
  return (
    <footer className="bg-navy text-slate-300 mt-9 pt-10">
      <div className="max-w-[1180px] mx-auto px-5 grid grid-cols-2 md:grid-cols-4 gap-7">
        <div>
          <LogoMark className="w-14 h-12 mb-1" />
          <h4 className="text-white font-semibold mb-2">JL ENTERPRISES</h4>
          <p className="text-[13px] leading-relaxed text-slate-400 max-w-[260px]">{ADDRESS}</p>
          <div className="flex gap-2 mt-3 flex-wrap text-[12px]">
            {["💳 Razorpay", "UPI", "Cards", "EMI", "COD"].map((p) => (
              <span key={p} className="bg-white/10 rounded px-2.5 py-1">{p}</span>
            ))}
          </div>
        </div>

        <div>
          <h4 className="text-white font-semibold mb-3">📞 Call Us</h4>
          {CONTACT_LIST.map((c) => (
            <a key={c.phone} href={telHref(c.phone)} className="block text-slate-400 hover:text-white text-[13px] py-1.5">
              <span className="block text-[11px] text-brand-sky uppercase tracking-wide">{c.label}</span>
              {prettyPhone(c.phone)}
            </a>
          ))}
        </div>

        <div>
          <h4 className="text-white font-semibold mb-3">Help</h4>
          <Link href="/login" className="block text-slate-400 hover:text-white text-[13px] py-1">Track Order</Link>
          <Link href="/service" className="block text-slate-400 hover:text-white text-[13px] py-1">Book Service / AMC</Link>
          {["Delivery & Returns", "EMI & Payment", "Contact Us"].map((x) => (
            <span key={x} className="block text-slate-400 text-[13px] py-1">{x}</span>
          ))}
        </div>

        <div>
          <h4 className="text-white font-semibold mb-3">Company</h4>
          {["About JL Enterprises", "Our Brands", "Terms & Conditions", "Privacy Policy", "Feedback"].map((x) => (
            <span key={x} className="block text-slate-400 text-[13px] py-1">{x}</span>
          ))}
        </div>
      </div>
      <div className="border-t border-white/10 mt-7 py-4 text-center text-[13px] text-slate-500">
        © 2026 JL Enterprises, Thoothukudi · Built on Next.js + Supabase
      </div>
    </footer>
  );
}
