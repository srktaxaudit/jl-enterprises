"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import type { Product } from "@/lib/types";
import { useCart } from "@/lib/cart";

export default function ProductBuy({ p }: { p: Product }) {
  const { add } = useCart();
  const router = useRouter();

  return (
    <div className="flex gap-3 flex-wrap mt-4.5">
      <button
        onClick={() => add(p)}
        disabled={p.stock === 0}
        className="bg-navy hover:bg-navy-600 disabled:bg-slate-300 text-white font-bold px-6 py-3 rounded-full"
      >
        🛒 Add to Cart
      </button>
      <button
        onClick={() => { add(p); router.push("/cart"); }}
        disabled={p.stock === 0}
        className="bg-gradient-to-br from-orange to-amber disabled:opacity-50 text-white font-bold px-6 py-3 rounded-full"
      >
        ⚡ Buy Now
      </button>
      <Link href="/service" className="bg-teal-700 text-white font-bold px-6 py-3 rounded-full">
        🔧 Book Installation
      </Link>
    </div>
  );
}
