"use client";

import Link from "next/link";
import { useCart } from "@/lib/cart";
import { inr } from "@/lib/format";
import { PRIMARY_WHATSAPP, waHref } from "@/lib/contact";

export default function FloatingCart() {
  const { count, subtotal } = useCart();

  return (
    <>
      {/* WhatsApp */}
      <a
        href={waHref(PRIMARY_WHATSAPP, "Hi JL Enterprises, I have a question about a product.")}
        target="_blank"
        className="fixed left-5 bottom-5 w-[54px] h-[54px] rounded-full bg-[#25d366] flex items-center justify-center text-2xl shadow-xl z-50"
        aria-label="Chat on WhatsApp"
      >
        💬
      </a>

      {/* Visitor count (static demo) */}
      <div className="fixed left-5 bottom-[86px] bg-white border border-slate-200 rounded-full px-3 py-1.5 text-[12px] text-slate-600 shadow-md z-50 flex items-center gap-2">
        <span className="w-2 h-2 rounded-full bg-green-500" /> 1,284 visitors today
      </div>

      {/* Floating cart */}
      {count > 0 && (
        <Link
          href="/cart"
          className="fixed right-5 bottom-5 bg-navy text-white rounded-2xl px-4 py-3 flex items-center gap-3 shadow-2xl z-50"
        >
          <div className="relative text-2xl">
            🛒
            <span className="absolute -top-2 -right-2.5 bg-orange text-[11px] font-bold w-[18px] h-[18px] rounded-full flex items-center justify-center">
              {count}
            </span>
          </div>
          <div>
            <b className="text-[15px]">{inr(subtotal)}</b>
            <span className="block text-[11px] text-brand-sky">
              {count} item{count > 1 ? "s" : ""} in cart
            </span>
          </div>
          <span className="bg-orange px-3.5 py-2 rounded-lg font-bold text-[13px]">View Cart →</span>
        </Link>
      )}
    </>
  );
}
