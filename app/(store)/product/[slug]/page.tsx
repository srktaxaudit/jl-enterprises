import Link from "next/link";
import { notFound } from "next/navigation";
import { fetchProductBySlug } from "@/lib/data";
import { inr, pctOff } from "@/lib/format";
import ProductBuy from "@/components/ProductBuy";

export default async function ProductPage({ params }: { params: { slug: string } }) {
  const p = await fetchProductBySlug(params.slug);
  if (!p) notFound();
  const off = pctOff(p.mrp, p.price);
  const low = p.stock > 0 && p.stock <= 3;

  return (
    <div className="max-w-[1180px] mx-auto px-5 animate-fade">
      <div className="text-[13px] text-slate-400 my-4">
        <Link href="/" className="text-brand">Home</Link> /{" "}
        <Link href="/category" className="text-brand">Products</Link> / {p.name}
      </div>

      <div className="grid md:grid-cols-[420px_1fr] gap-9 my-3.5 mb-8">
        <div className="bg-white border border-slate-200 rounded-2xl p-8 flex items-center justify-center text-[8rem] min-h-[340px] overflow-hidden">
          {p.imageUrl ? <img src={p.imageUrl} alt={p.name} className="max-h-[300px] object-contain" /> : p.emoji}
        </div>
        <div>
          <div className="text-[11px] text-slate-400 uppercase font-semibold tracking-wide">{p.brand}</div>
          <h1 className="text-2xl text-navy font-bold mb-1.5">{p.name}</h1>
          <div className="text-amber text-sm mb-1">
            {"★".repeat(Math.round(p.rating))}
            <span className="text-slate-400"> ({p.reviewCount} reviews)</span>
          </div>

          <div className="flex items-baseline gap-2.5 my-3.5">
            <span className="text-3xl font-extrabold text-navy">{inr(p.price)}</span>
            {p.mrp > p.price && <span className="line-through text-slate-400">{inr(p.mrp)}</span>}
            {off > 0 && <span className="text-green-600 font-bold">{off}% off</span>}
          </div>
          <div className="text-[14px] text-brand mb-1">EMI from {inr(p.emiPerMonth)}/mo · No-cost EMI available</div>
          <div className={`text-sm font-bold ${low ? "text-orange-600" : "text-green-600"}`}>
            {p.stock === 0 ? "✖ Out of stock" : low ? `⚠ Only ${p.stock} left — order soon` : "✔ In stock — delivery in 2 days"}
          </div>

          {p.description && <p className="text-slate-600 text-sm mt-4 leading-relaxed">{p.description}</p>}

          {p.specs && (
            <div className="my-4.5 border border-slate-200 rounded-xl overflow-hidden mt-4">
              {Object.entries(p.specs).map(([k, v]) => (
                <div key={k} className="flex px-3.5 py-2.5 text-sm border-b border-slate-100 last:border-0">
                  <b className="w-40 text-slate-400 font-semibold">{k}</b>
                  <span className="text-slate-700">{v}</span>
                </div>
              ))}
            </div>
          )}

          <ProductBuy p={p} />

          <div className="flex gap-4 mt-5 flex-wrap text-[13px] text-slate-600">
            <span>🚚 Free Tamil Nadu delivery</span>
            <span>♻️ Exchange available</span>
            <span>🛡️ Brand warranty</span>
            <span>💳 COD &amp; EMI</span>
          </div>
        </div>
      </div>
    </div>
  );
}
