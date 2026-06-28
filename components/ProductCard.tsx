"use client";

import Link from "next/link";
import type { Product } from "@/lib/types";
import { inr, pctOff } from "@/lib/format";
import { useCart } from "@/lib/cart";

export default function ProductCard({ p }: { p: Product }) {
  const { add } = useCart();
  const off = pctOff(p.mrp, p.price);
  const low = p.stock > 0 && p.stock <= 3;

  return (
    <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden hover:shadow-card hover:-translate-y-0.5 transition relative">
      {off > 0 && (
        <span className="absolute top-3 left-3 bg-red-600 text-white text-[11px] font-bold px-2 py-0.5 rounded z-[2]">
          -{off}%
        </span>
      )}
      <Link href={`/product/${p.slug}`} className="block h-40 flex items-center justify-center text-6xl bg-gradient-to-br from-slate-50 to-slate-100 overflow-hidden">
        {p.imageUrl ? <img src={p.imageUrl} alt={p.name} className="w-full h-full object-contain" /> : p.emoji}
      </Link>
      <div className="p-3.5">
        <div className="text-[11px] text-slate-400 uppercase font-semibold tracking-wide">{p.brand}</div>
        <Link href={`/product/${p.slug}`} className="block text-[15px] text-navy font-semibold mt-0.5 mb-1.5 leading-snug h-9 overflow-hidden">
          {p.name}
        </Link>
        <div className="text-amber text-[13px] mb-2">
          {"★".repeat(Math.round(p.rating))}
          <span className="text-slate-400"> ({p.reviewCount})</span>
        </div>
        <div className={`text-[11px] font-bold mb-2 ${low ? "text-orange-600" : "text-green-600"}`}>
          {p.stock === 0 ? "Out of stock" : low ? `Only ${p.stock} left` : "In stock"}
        </div>
        <div className="flex items-baseline gap-2 mb-0.5">
          <span className="text-xl font-extrabold text-navy">{inr(p.price)}</span>
          {p.mrp > p.price && <span className="text-[13px] text-slate-400 line-through">{inr(p.mrp)}</span>}
        </div>
        <div className="text-[12px] text-brand mb-2.5">EMI from {inr(p.emiPerMonth)}/mo</div>
        <button
          onClick={() => add(p)}
          disabled={p.stock === 0}
          className="w-full bg-navy hover:bg-orange disabled:bg-slate-300 text-white font-bold py-2.5 rounded-lg text-sm transition"
        >
          🛒 Add to Cart
        </button>
      </div>
    </div>
  );
}
