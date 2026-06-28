"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { Product } from "@/lib/types";

type Cat = { slug: string; name: string };

export default function ProductForm({
  initial,
  categories,
}: {
  initial?: Product;
  categories: Cat[];
}) {
  const router = useRouter();
  const editing = Boolean(initial);
  const [f, setF] = useState({
    name: initial?.name ?? "",
    brand: initial?.brand ?? "",
    categorySlug: initial?.categorySlug ?? categories[0]?.slug ?? "",
    price: initial?.price ?? 0,
    mrp: initial?.mrp ?? 0,
    stock: initial?.stock ?? 0,
    emoji: initial?.emoji ?? "📦",
    imageUrl: initial?.imageUrl ?? "",
    description: initial?.description ?? "",
    isFeatured: initial?.isFeatured ?? false,
    isActive: initial?.isActive ?? true,
  });
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [msg, setMsg] = useState("");

  const set = (k: string, v: any) => setF((p) => ({ ...p, [k]: v }));

  async function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    const fd = new FormData();
    fd.append("file", file);
    const res = await fetch("/api/admin/upload", { method: "POST", body: fd });
    const data = await res.json();
    setUploading(false);
    if (data.url) { set("imageUrl", data.url); setMsg("Image uploaded ✅"); }
    else if (data.demo) setMsg("Demo mode — connect Supabase Storage to upload. Paste an image URL or use an emoji for now.");
    else setMsg(data.error ?? "Upload failed");
  }

  async function save() {
    if (!f.name || !f.price) { setMsg("Name and price are required."); return; }
    setBusy(true);
    const res = await fetch("/api/admin/products", {
      method: editing ? "PATCH" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(editing ? { id: initial!.id, ...f } : f),
    });
    const data = await res.json();
    setBusy(false);
    if (res.ok) {
      router.push("/admin/products");
      router.refresh();
    } else {
      setMsg(data.error ?? "Save failed");
    }
  }

  return (
    <div className="bg-white border border-slate-200 rounded-2xl p-5 max-w-[760px]">
      <div className="grid md:grid-cols-[120px_1fr] gap-5">
        <div>
          <div className="w-[120px] h-[120px] rounded-xl bg-gradient-to-br from-slate-50 to-slate-100 flex items-center justify-center overflow-hidden border border-slate-200">
            {f.imageUrl
              ? <img src={f.imageUrl} alt="" className="w-full h-full object-cover" />
              : <span className="text-5xl">{f.emoji}</span>}
          </div>
          <label className="block mt-2 text-[12px] text-brand font-semibold cursor-pointer">
            {uploading ? "Uploading…" : "⬆ Upload image"}
            <input type="file" accept="image/*" className="hidden" onChange={onFile} />
          </label>
          <input value={f.emoji} onChange={(e) => set("emoji", e.target.value)} className="w-full border border-slate-200 rounded-lg px-2 py-1.5 text-sm text-center mt-1" placeholder="emoji" />
        </div>

        <div>
          <Field label="Product Name *"><input value={f.name} onChange={(e) => set("name", e.target.value)} className="ipt" /></Field>
          <div className="flex gap-3">
            <Field label="Brand"><input value={f.brand} onChange={(e) => set("brand", e.target.value)} className="ipt" /></Field>
            <Field label="Category">
              <select value={f.categorySlug} onChange={(e) => set("categorySlug", e.target.value)} className="ipt">
                {categories.map((c) => <option key={c.slug} value={c.slug}>{c.name}</option>)}
              </select>
            </Field>
          </div>
          <div className="flex gap-3">
            <Field label="Price ₹ *"><input type="number" value={f.price} onChange={(e) => set("price", e.target.value)} className="ipt" /></Field>
            <Field label="MRP ₹"><input type="number" value={f.mrp} onChange={(e) => set("mrp", e.target.value)} className="ipt" /></Field>
            <Field label="Stock"><input type="number" value={f.stock} onChange={(e) => set("stock", e.target.value)} className="ipt" /></Field>
          </div>
          <Field label="Image URL (optional)"><input value={f.imageUrl} onChange={(e) => set("imageUrl", e.target.value)} className="ipt" placeholder="https://…" /></Field>
          <Field label="Description"><textarea rows={3} value={f.description} onChange={(e) => set("description", e.target.value)} className="ipt" /></Field>
          <div className="flex gap-5 mt-1">
            <label className="flex items-center gap-2 text-sm text-slate-600"><input type="checkbox" checked={f.isActive} onChange={(e) => set("isActive", e.target.checked)} /> Active (visible)</label>
            <label className="flex items-center gap-2 text-sm text-slate-600"><input type="checkbox" checked={f.isFeatured} onChange={(e) => set("isFeatured", e.target.checked)} /> Featured</label>
          </div>
        </div>
      </div>

      {msg && <p className="text-[13px] text-orange-600 mt-3">{msg}</p>}
      <div className="flex gap-3 mt-4">
        <button onClick={save} disabled={busy} className="bg-navy disabled:opacity-60 text-white font-bold px-6 py-2.5 rounded-lg">
          {busy ? "Saving…" : editing ? "Save Changes" : "Create Product"}
        </button>
        <button onClick={() => router.push("/admin/products")} className="border border-slate-200 text-slate-600 font-bold px-5 py-2.5 rounded-lg">Cancel</button>
      </div>

      <style>{`.ipt{width:100%;border:1px solid #e2e8f0;border-radius:8px;padding:9px 12px;font-size:14px;color:#475569;font-family:inherit}`}</style>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="mb-3 flex-1">
      <label className="text-[12px] text-slate-400 block mb-1.5">{label}</label>
      {children}
    </div>
  );
}
