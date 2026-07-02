import Link from "next/link";
import { fetchProducts } from "@/lib/data";
import { applyFilters, collectBrands, type FilterParams } from "@/lib/filters";
import ProductCard from "@/components/ProductCard";
import FilterBar from "@/components/FilterBar";

export const metadata = { title: "Search — JL Enterprises" };

export default async function SearchPage({
  searchParams,
}: {
  searchParams: FilterParams;
}) {
  const q = (searchParams.q ?? "").trim();
  const all = await fetchProducts();
  const matched = q ? applyFilters(all, { q }) : all;
  const results = applyFilters(all, searchParams);

  return (
    <div className="max-w-[1180px] mx-auto px-5 animate-fade">
      <div className="text-[13px] text-slate-400 my-4">
        <Link href="/" className="text-brand">Home</Link> / Search
      </div>

      <h2 className="text-2xl text-navy font-bold mb-1">
        {q ? <>Results for &ldquo;{q}&rdquo;</> : "Search"}
      </h2>
      <p className="text-slate-400 text-sm mb-4">
        {q
          ? `${results.length} product${results.length === 1 ? "" : "s"} found`
          : "Type in the search box above to find products."}
      </p>

      {q && <FilterBar brands={collectBrands(matched)} />}

      {q && results.length === 0 ? (
        <div className="text-center py-16 text-slate-400">
          Nothing matched your search{" "}
          <div className="mt-4">
            <Link href="/category" className="bg-navy text-white px-6 py-2.5 rounded-full font-semibold">
              Browse All Products
            </Link>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          {results.map((p) => <ProductCard key={p.id} p={p} />)}
        </div>
      )}
    </div>
  );
}
