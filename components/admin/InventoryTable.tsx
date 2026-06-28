"use client";

import { useState } from "react";
import type { Product } from "@/lib/types";
import { ToastHost, useToast } from "@/components/admin/Toast";

function Row({ p }: { p: Product }) {
  const [stock, setStock] = useState(p.stock);
  const [busy, setBusy] = useState(false);
  const toast = useToast();
  const status = stock === 0 ? ["Out", "bg-pink-100 text-pink-700"] : stock <= 3 ? ["Low", "bg-amber-100 text-amber-700"] : ["Healthy", "bg-green-100 text-green-700"];

  async function save(next: number) {
    next = Math.max(0, next);
    setStock(next);
    setBusy(true);
    await fetch("/api/admin/inventory", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id: p.id, stock: next }),
    });
    setBusy(false);
    toast(`${p.brand} stock set to ${next}`);
  }

  return (
    <tr className="border-t border-slate-100">
      <td className="py-3"><b className="text-navy text-[14px]">{p.name}</b></td>
      <td className="text-slate-600">{p.brand}</td>
      <td>
        <div className="inline-flex items-center gap-2 border border-slate-200 rounded-lg px-2 py-1">
          <button onClick={() => save(stock - 1)} disabled={busy} className="text-navy text-lg w-5">−</button>
          <span className="w-8 text-center font-bold">{stock}</span>
          <button onClick={() => save(stock + 1)} disabled={busy} className="text-navy text-lg w-5">+</button>
        </div>
      </td>
      <td className="text-slate-500">{p.stock <= 3 ? 5 : 4}</td>
      <td><span className={`text-[11px] font-bold px-2.5 py-1 rounded-full ${status[1]}`}>{status[0]}</span></td>
    </tr>
  );
}

export default function InventoryTable({ products }: { products: Product[] }) {
  const value = products.reduce((s, p) => s + p.price * p.stock, 0);
  return (
    <ToastHost>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-5">
        <div className="bg-white border border-slate-200 rounded-xl p-4"><div className="text-[12px] text-slate-400 uppercase">SKUs</div><div className="text-2xl font-extrabold text-navy mt-1">{products.length}</div></div>
        <div className="bg-white border border-slate-200 rounded-xl p-4"><div className="text-[12px] text-slate-400 uppercase">Stock Value</div><div className="text-2xl font-extrabold text-navy mt-1">₹{Math.round(value).toLocaleString("en-IN")}</div></div>
        <div className="bg-white border border-slate-200 rounded-xl p-4"><div className="text-[12px] text-slate-400 uppercase">Low Stock</div><div className="text-2xl font-extrabold text-orange-600 mt-1">{products.filter((p) => p.stock > 0 && p.stock <= 3).length}</div></div>
        <div className="bg-white border border-slate-200 rounded-xl p-4"><div className="text-[12px] text-slate-400 uppercase">Out of Stock</div><div className="text-2xl font-extrabold text-red-600 mt-1">{products.filter((p) => p.stock === 0).length}</div></div>
      </div>
      <div className="bg-white border border-slate-200 rounded-2xl p-5">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-navy font-semibold">Stock Register</h3>
          <button className="bg-navy text-white text-[13px] font-bold px-4 py-2 rounded-lg">+ Stock In</button>
        </div>
        <div className="overflow-x-auto"><table className="w-full text-sm min-w-[560px]">
          <thead><tr className="text-left text-slate-400 text-[12px] uppercase">
            <th className="py-2">Product</th><th>Brand</th><th>In Stock</th><th>Reorder At</th><th>Status</th>
          </tr></thead>
          <tbody>{products.map((p) => <Row key={p.id} p={p} />)}</tbody>
        </table></div>
        <p className="text-[12px] text-slate-400 mt-3">Adjust stock with − / + . Saves instantly; storefront reflects it on next load.</p>
      </div>
    </ToastHost>
  );
}
