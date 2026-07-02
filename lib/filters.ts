import type { Product } from "./types";

// ════════════════════════════════════════════════════════════════════
//  Catalog filtering / sorting shared by /category, /category/[slug]
//  and /search. Driven entirely by URL search params so filtered
//  views are shareable and back-button friendly.
// ════════════════════════════════════════════════════════════════════

export type FilterParams = {
  q?: string;
  brand?: string;
  min?: string; // min price (₹)
  max?: string; // max price (₹)
  rating?: string; // minimum rating, e.g. "4"
  sort?: string; // relevance | price_asc | price_desc | rating | discount
};

export const SORT_OPTIONS = [
  { value: "", label: "Featured" },
  { value: "price_asc", label: "Price: Low to High" },
  { value: "price_desc", label: "Price: High to Low" },
  { value: "rating", label: "Top Rated" },
  { value: "discount", label: "Biggest Discount" },
] as const;

/** Case-insensitive match across name / brand / description / category. */
export function matchesQuery(p: Product, q: string): boolean {
  const hay = `${p.name} ${p.brand} ${p.description ?? ""} ${p.categorySlug}`.toLowerCase();
  return q
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
    .every((term) => hay.includes(term));
}

export function applyFilters(products: Product[], f: FilterParams): Product[] {
  let list = products;

  const q = (f.q ?? "").trim();
  if (q) list = list.filter((p) => matchesQuery(p, q));

  if (f.brand) {
    const b = f.brand.toLowerCase();
    list = list.filter((p) => p.brand.toLowerCase() === b);
  }

  const min = Number(f.min);
  if (Number.isFinite(min) && f.min) list = list.filter((p) => p.price >= min);
  const max = Number(f.max);
  if (Number.isFinite(max) && f.max) list = list.filter((p) => p.price <= max);

  const rating = Number(f.rating);
  if (Number.isFinite(rating) && f.rating) list = list.filter((p) => p.rating >= rating);

  switch (f.sort) {
    case "price_asc":
      list = [...list].sort((a, b) => a.price - b.price);
      break;
    case "price_desc":
      list = [...list].sort((a, b) => b.price - a.price);
      break;
    case "rating":
      list = [...list].sort((a, b) => b.rating - a.rating);
      break;
    case "discount":
      list = [...list].sort(
        (a, b) => (b.mrp - b.price) / (b.mrp || 1) - (a.mrp - a.price) / (a.mrp || 1)
      );
      break;
  }
  return list;
}

/** Distinct brands present in a product list (for the brand dropdown). */
export function collectBrands(products: Product[]): string[] {
  return Array.from(new Set(products.map((p) => p.brand).filter(Boolean))).sort();
}

/** True when any filter beyond the search query is active. */
export function hasActiveFilters(f: FilterParams): boolean {
  return Boolean(f.brand || f.min || f.max || f.rating || f.sort);
}
