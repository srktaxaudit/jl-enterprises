"use client";

import Link from "next/link";
import { useWishlist } from "@/lib/wishlist";
import { useCart } from "@/lib/cart";
import { inr } from "@/lib/format";

export default function WishlistPage() {
  const { items, remove } = useWishlist();
  const { add } = useCart();

  if (items.length === 0) {
    return (
      <div className="max-w-[1180px] mx-auto px-5 text-center py-20 text-slate-400 animate-fade">
        <span className="text-5xl block mb-3">❤️</span>
        Your wishlist is empty — tap the heart on any product to save it here.
        <div className="mt-4">
          <Link href="/category" className="bg-navy text-white px-6 py-2.5 rounded-full font-semibold">
            Browse Products
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-[1180px] mx-auto px-5 animate-fade">
      <h2 className="text-2xl text-navy font-bold my-5">❤️ My Wishlist ({items.length})</h2>
      <div className="grid gap-3 mb-8">
        {items.map((p) => (
          <div key={p.id} className="bg-white border border-slate-200 rounded-2xl p-4 flex items-center gap-4 flex-wrap">
            <Link href={`/product/${p.slug}`} className="w-16 h-16 flex items-center justify-center text-4xl bg-slate-50 rounded-xl overflow-hidden shrink-0">
              {p.imageUrl ? <img src={p.imageUrl} alt={p.name} className="w-full h-full object-contain" /> : p.emoji}
            </Link>
            <div className="flex-1 min-w-[180px]">
              <div className="text-[11px] text-slate-400 uppercase font-semibold">{p.brand}</div>
              <Link href={`/product/${p.slug}`} className="text-navy font-semibold leading-snug block">
                {p.name}
              </Link>
              <div className="text-lg font-extrabold text-navy mt-0.5">
                {inr(p.price)}{" "}
                {p.mrp > p.price && <span className="text-[13px] text-slate-400 line-through font-normal">{inr(p.mrp)}</span>}
              </div>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => { add(p); remove(p.id); }}
                disabled={p.stock === 0}
                className="bg-navy hover:bg-orange disabled:bg-slate-300 text-white font-bold px-4 py-2.5 rounded-full text-sm transition"
              >
                {p.stock === 0 ? "Out of stock" : "Move to Cart 🛒"}
              </button>
              <button
                onClick={() => remove(p.id)}
                className="border border-slate-200 text-slate-500 hover:text-red-500 hover:border-red-200 px-3.5 py-2.5 rounded-full text-sm font-semibold"
              >
                ✕
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
