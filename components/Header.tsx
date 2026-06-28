"use client";

import Link from "next/link";
import { useCart } from "@/lib/cart";
import { getCategories } from "@/lib/catalog";
import { LogoMark } from "@/components/Logo";

export default function Header() {
  const { count } = useCart();
  const cats = getCategories();

  return (
    <>
      <div className="bg-navy text-white text-center text-[13px] py-1.5 px-3">
        🚚 Free door delivery across <b className="text-amber">Tamil Nadu</b> · 💳 Easy EMI &amp; Cash on Delivery · 🔧 Doorstep service &amp; AMC
      </div>

      <header className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-[1180px] mx-auto px-5 flex items-center gap-5 py-3">
          <Link href="/" className="flex items-center gap-2.5 shrink-0">
            <LogoMark className="w-12 h-10" />
            <div className="leading-tight hidden sm:block">
              <div className="font-extrabold text-navy">JL ENTERPRISES</div>
              <div className="text-[10px] font-semibold text-slate-400 tracking-wider">
                HOME APPLIANCES · THOOTHUKUDI
              </div>
            </div>
          </Link>

          <div className="flex-1 flex bg-slate-100 border border-slate-200 rounded-full overflow-hidden">
            <input
              className="flex-1 bg-transparent px-4 py-2.5 text-sm outline-none text-slate-600"
              placeholder="Search for AC, TV, Fridge, Washing Machine, Furniture…"
            />
            <Link href="/category" className="bg-navy text-white px-5 flex items-center font-semibold text-sm">
              Search
            </Link>
          </div>

          <nav className="flex items-center gap-4 text-slate-600">
            <Link href="/service" className="flex flex-col items-center text-[11px]">
              <span className="text-xl">🔧</span>Service
            </Link>
            <Link href="/login" className="hidden sm:flex flex-col items-center text-[11px]">
              <span className="text-xl">👤</span>Login
            </Link>
            <Link href="/cart" className="relative flex flex-col items-center text-[11px]">
              <span className="text-xl">🛒</span>Cart
              {count > 0 && (
                <span className="absolute -top-1.5 -right-2 bg-orange text-white text-[11px] font-bold w-[18px] h-[18px] rounded-full flex items-center justify-center">
                  {count}
                </span>
              )}
            </Link>
          </nav>
        </div>

        <nav className="bg-navy-600">
          <div className="max-w-[1180px] mx-auto px-5 flex items-center gap-1 overflow-x-auto no-scrollbar">
            <Link href="/category" className="bg-orange text-white px-4 py-2.5 text-sm font-bold whitespace-nowrap">
              ☰ All
            </Link>
            {cats.map((c) => (
              <Link
                key={c.slug}
                href={`/category/${c.slug}`}
                className="text-blue-100/90 hover:text-white px-4 py-2.5 text-sm whitespace-nowrap border-b-[3px] border-transparent hover:border-orange"
              >
                {c.name}
              </Link>
            ))}
          </div>
        </nav>
      </header>
    </>
  );
}
