"use client";

import { useState } from "react";
import Link from "next/link";
import type { Product } from "@/lib/types";
import { inr } from "@/lib/format";
import { ToastHost, useToast } from "@/components/admin/Toast";

function Row({ p }: { p: Product }) {
  const [active, setActive] = useState(p.isActive);
  const [busy, setBusy] = useState(false);
  const toast = useToast();

  async function toggle() {
    const next = !active;
    setActive(next);
    setBusy(true);
    const res = await fetch("/api/admin/products", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id: p.id, isActive: next }),
    });
    setBusy(false);
    toast(next ? `${p.brand} ${p.name.split(" ").slice(0, 2).join(" ")} enabled` : `Disabled — hidden from store`);
    if (!res.ok) setActive(!next);
  }

  return (
    <tr className="border-t border-slate-100">
      <td className="py-3 text-2xl">{p.emoji}</td>
      <td>
        <b className="text-navy text-[14px] block">{p.name}</b>
        <span className="text-[12px] text-slate-400">{p.brand}</span>
      </td>
      <td className="font-bold text-navy">{inr(p.price)}</td>
      <td className={`font-bold ${p.stock <= 3 ? "text-orange-600" : "text-green-600"}`}>{p.stock}</td>
      <td>
        <button
          onClick={toggle}
          disabled={busy}
          className={`relative w-[42px] h-[23px] rounded-full transition ${active ? "bg-green-500" : "bg-slate-300"}`}
          aria-label="toggle active"
        >
          <span className={`absolute top-0.5 w-[19px] h-[19px] rounded-full bg-white transition-all ${active ? "left-[21px]" : "left-0.5"}`} />
        </button>
      </td>
      <td className="text-[12px] font-semibold">{active ? <span className="text-green-600">Live</span> : <span className="text-slate-400">Hidden</span>}</td>
      <td><Link href={`/admin/products/${p.id}/edit`} className="text-brand text-[13px] font-semibold">Edit</Link></td>
    </tr>
  );
}

export default function ProductsTable({ products }: { products: Product[] }) {
  return (
    <ToastHost>
      <div className="bg-white border border-slate-200 rounded-2xl p-5">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-navy font-semibold">{products.length} Products</h3>
          <Link href="/admin/products/new" className="bg-navy text-white text-[13px] font-bold px-4 py-2 rounded-lg">+ Add Product</Link>
        </div>
        <div className="overflow-x-auto"><table className="w-full text-sm min-w-[620px]">
          <thead><tr className="text-left text-slate-400 text-[12px] uppercase">
            <th className="py-2"></th><th>Product</th><th>Price</th><th>Stock</th><th>Enabled</th><th>Status</th><th></th>
          </tr></thead>
          <tbody>
            {products.map((p) => <Row key={p.id} p={p} />)}
          </tbody>
        </table></div>
        <p className="text-[12px] text-slate-400 mt-3">Toggle the switch to instantly show/hide a product on the storefront.</p>
      </div>
    </ToastHost>
  );
}
