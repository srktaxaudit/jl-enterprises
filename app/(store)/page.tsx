import Link from "next/link";
import { fetchCategories, fetchFeatured, fetchProducts } from "@/lib/data";
import ProductCard from "@/components/ProductCard";

export default async function HomePage() {
  const [cats, deals, all] = await Promise.all([
    fetchCategories(),
    fetchFeatured(),
    fetchProducts(),
  ]);
  const furniture = all
    .filter((p) => ["furniture", "kitchen", "home-theatre"].includes(p.categorySlug))
    .slice(0, 4);

  return (
    <div className="max-w-[1180px] mx-auto px-5 animate-fade">
      {/* HERO */}
      <section className="grid md:grid-cols-[1fr_320px] gap-4 my-5">
        <div className="bg-gradient-to-br from-navy via-navy-600 to-brand rounded-3xl p-10 text-white relative overflow-hidden min-h-[280px] flex flex-col justify-center">
          <span className="inline-block bg-orange text-white text-[12px] font-bold px-3 py-1.5 rounded-full w-fit mb-3.5">
            🔥 SUMMER SALE · LIMITED PERIOD
          </span>
          <h1 className="text-4xl leading-tight max-w-[430px] mb-2.5 font-bold">
            Beat the Heat with <span className="text-amber">Up to 40% Off</span> on ACs
          </h1>
          <p className="text-blue-100 max-w-[380px] mb-5">
            Top brands — Voltas, LG, Samsung, Daikin. Free installation &amp; door delivery across Tamil Nadu.
          </p>
          <Link href="/category/air-conditioners" className="bg-gradient-to-br from-orange to-amber text-white font-bold px-6 py-3 rounded-full w-fit">
            Shop Air Conditioners →
          </Link>
        </div>
        <div className="flex flex-col gap-4">
          <Link href="/category/furniture" className="flex-1 rounded-2xl p-5 text-white bg-gradient-to-br from-violet-600 to-purple-500 flex flex-col justify-center">
            <b className="text-lg">Furniture Fest</b>
            <span className="text-[13px] opacity-90">Sofas, beds &amp; chairs from ₹4,999</span>
          </Link>
          <Link href="/category" className="flex-1 rounded-2xl p-5 text-white bg-gradient-to-br from-green-600 to-green-500 flex flex-col justify-center">
            <b className="text-lg">Exchange Offer</b>
            <span className="text-[13px] opacity-90">Old appliance? Get extra ₹3,000 off</span>
          </Link>
        </div>
      </section>

      {/* USP */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3.5 mb-7">
        {[
          ["🚚", "Free Delivery", "Across Tamil Nadu"],
          ["💳", "Easy EMI & COD", "No-cost EMI options"],
          ["🔧", "Doorstep Service", "Repair & AMC booking"],
          ["🛡️", "Brand Warranty", "100% genuine products"],
        ].map(([e, t, s]) => (
          <div key={t} className="bg-white border border-slate-200 rounded-xl p-3.5 flex items-center gap-3">
            <span className="text-2xl">{e}</span>
            <div>
              <b className="text-sm text-navy block">{t}</b>
              <span className="text-[12px] text-slate-400">{s}</span>
            </div>
          </div>
        ))}
      </div>

      {/* CATEGORIES */}
      <section className="mb-8">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-2xl text-navy font-bold">Shop by Category</h2>
          <Link href="/category" className="text-orange text-sm font-semibold">View all →</Link>
        </div>
        <div className="grid grid-cols-3 md:grid-cols-7 gap-3.5">
          {cats.map((c) => (
            <Link key={c.slug} href={`/category/${c.slug}`} className="bg-white border border-slate-200 rounded-2xl py-4 px-2 text-center hover:shadow-card hover:-translate-y-0.5 transition">
              <span className="text-3xl block mb-2">{c.emoji}</span>
              <b className="text-[13px] text-navy">{c.name}</b>
            </Link>
          ))}
        </div>
      </section>

      {/* DEALS */}
      <section className="mb-8">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-2xl text-navy font-bold">🔥 Deals of the Day</h2>
          <Link href="/category" className="text-orange text-sm font-semibold">See all deals →</Link>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {deals.map((p) => <ProductCard key={p.id} p={p} />)}
        </div>
      </section>

      {/* SERVICE BANNER */}
      <section className="mb-8">
        <div className="bg-gradient-to-br from-teal-700 to-teal-500 rounded-3xl px-9 py-7 text-white flex items-center gap-6 flex-wrap">
          <span className="text-5xl">🔧</span>
          <div className="flex-1 min-w-[240px]">
            <h2 className="text-2xl font-bold mb-1.5">Need a repair or AMC? Book service at your doorstep</h2>
            <p className="text-teal-50 text-sm">
              AC, Fridge, Washing Machine, TV &amp; more — our in-house &amp; partner technicians come to you anywhere in Tamil Nadu.
            </p>
          </div>
          <Link href="/service" className="bg-white text-teal-700 font-bold px-6 py-3 rounded-full whitespace-nowrap">
            Book Service →
          </Link>
        </div>
      </section>

      {/* FURNITURE & HOME */}
      <section className="mb-8">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-2xl text-navy font-bold">Furniture &amp; Home</h2>
          <Link href="/category" className="text-orange text-sm font-semibold">View all →</Link>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {furniture.map((p) => <ProductCard key={p.id} p={p} />)}
        </div>
      </section>
    </div>
  );
}
