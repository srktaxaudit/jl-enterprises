"use client";

import Link from "next/link";
import { useCart } from "@/lib/cart";
import { inr } from "@/lib/format";

const EXCHANGE_BONUS = 3000;

export default function CartPage() {
  const { lines, subtotal, count, setQty, remove } = useCart();
  const total = Math.max(0, subtotal - (count > 0 ? EXCHANGE_BONUS : 0));

  return (
    <div className="max-w-[1180px] mx-auto px-5 animate-fade">
      <Link href="/category" className="inline-flex items-center gap-1.5 text-brand text-sm font-semibold my-4">
        ← Continue shopping
      </Link>
      <h2 className="text-2xl text-navy font-bold mb-4">🛒 Your Cart</h2>

      {count === 0 ? (
        <div className="text-center py-16 text-slate-400">
          <span className="text-5xl block mb-3">🛒</span>
          Your cart is empty
          <div className="mt-4">
            <Link href="/category" className="bg-navy text-white px-6 py-2.5 rounded-full font-semibold">
              Browse Products
            </Link>
          </div>
        </div>
      ) : (
        <div className="grid md:grid-cols-[1fr_340px] gap-6 mb-8">
          <div className="bg-white border border-slate-200 rounded-2xl p-5">
            <h3 className="text-navy font-semibold mb-3">{count} Item(s)</h3>
            {lines.map(({ product: p, qty }) => (
              <div key={p.id} className="flex gap-3.5 items-center py-3.5 border-b border-slate-100 last:border-0">
                <div className="w-16 h-16 rounded-xl bg-gradient-to-br from-slate-50 to-slate-100 flex items-center justify-center text-3xl shrink-0">
                  {p.emoji}
                </div>
                <div className="flex-1">
                  <b className="text-navy text-[15px] block">{p.name}</b>
                  <span className="text-[13px] text-slate-400">{p.brand} · {inr(p.price)}</span>
                  <button onClick={() => remove(p.id)} className="block text-[12px] text-red-500 mt-1">Remove</button>
                </div>
                <div className="flex items-center gap-2.5 border border-slate-200 rounded-lg px-2 py-1">
                  <button onClick={() => setQty(p.id, qty - 1)} className="text-navy text-lg w-4">−</button>
                  {qty}
                  <button onClick={() => setQty(p.id, qty + 1)} className="text-navy text-lg w-4">+</button>
                </div>
                <div className="font-extrabold text-navy w-24 text-right">{inr(p.price * qty)}</div>
              </div>
            ))}
          </div>

          <div className="bg-white border border-slate-200 rounded-2xl p-5 h-fit">
            <h3 className="text-navy font-semibold mb-4">Order Summary</h3>
            <div className="flex justify-between py-2 text-sm text-slate-600">
              <span>Subtotal</span><span>{inr(subtotal)}</span>
            </div>
            <div className="flex justify-between py-2 text-sm text-slate-600">
              <span>Delivery (Tamil Nadu)</span><span className="text-green-600">FREE</span>
            </div>
            <div className="flex justify-between py-2 text-sm text-slate-600">
              <span>Exchange bonus</span><span className="text-green-600">- {inr(EXCHANGE_BONUS)}</span>
            </div>
            <div className="flex justify-between border-t border-slate-200 mt-2 pt-3.5 text-lg font-extrabold text-navy">
              <span>Total</span><span>{inr(total)}</span>
            </div>
            <Link
              href="/checkout"
              className="block text-center w-full bg-gradient-to-br from-orange to-amber text-white font-bold py-3.5 rounded-full mt-3.5"
            >
              Proceed to Checkout →
            </Link>
            <p className="text-[12px] text-slate-400 text-center mt-2.5">EMI options available at checkout</p>
          </div>
        </div>
      )}
    </div>
  );
}
