import Link from "next/link";
import { fetchProducts, fetchCategories } from "@/lib/data";
import ProductCard from "@/components/ProductCard";

export default async function AllProductsPage() {
  const [products, cats] = await Promise.all([fetchProducts(), fetchCategories()]);

  return (
    <div className="max-w-[1180px] mx-auto px-5 animate-fade">
      <div className="text-[13px] text-slate-400 my-4">
        <Link href="/" className="text-brand">Home</Link> / Products
      </div>
      <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
        <h2 className="text-2xl text-navy font-bold">All Products</h2>
        <div className="flex gap-2 flex-wrap">
          {cats.map((c) => (
            <Link key={c.slug} href={`/category/${c.slug}`} className="text-[13px] border border-slate-200 bg-white rounded-full px-3 py-1.5 text-slate-600 hover:border-navy">
              {c.emoji} {c.name}
            </Link>
          ))}
        </div>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {products.map((p) => <ProductCard key={p.id} p={p} />)}
      </div>
    </div>
  );
}
