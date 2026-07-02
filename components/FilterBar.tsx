"use client";

import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { SORT_OPTIONS } from "@/lib/filters";

const PRICE_BANDS = [
  { label: "Any price", min: "", max: "" },
  { label: "Under ₹10,000", min: "", max: "10000" },
  { label: "₹10,000 – ₹25,000", min: "10000", max: "25000" },
  { label: "₹25,000 – ₹50,000", min: "25000", max: "50000" },
  { label: "Above ₹50,000", min: "50000", max: "" },
];

/** URL-param driven filters: price band, brand, min rating, sort.
 *  Server components re-render the filtered grid on every change. */
export default function FilterBar({ brands }: { brands: string[] }) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function update(patch: Record<string, string>) {
    const next = new URLSearchParams(params.toString());
    for (const [k, v] of Object.entries(patch)) {
      if (v) next.set(k, v);
      else next.delete(k);
    }
    router.replace(`${pathname}?${next.toString()}`, { scroll: false });
  }

  const priceIdx = PRICE_BANDS.findIndex(
    (b) => b.min === (params.get("min") ?? "") && b.max === (params.get("max") ?? "")
  );
  const active = Boolean(
    params.get("brand") || params.get("min") || params.get("max") || params.get("rating") || params.get("sort")
  );

  const sel =
    "border border-slate-200 bg-white rounded-full px-3 py-1.5 text-[13px] text-slate-600 outline-none";

  return (
    <div className="flex items-center gap-2 flex-wrap mb-4">
      <select
        aria-label="Price"
        className={sel}
        value={priceIdx === -1 ? 0 : priceIdx}
        onChange={(e) => {
          const b = PRICE_BANDS[Number(e.target.value)];
          update({ min: b.min, max: b.max });
        }}
      >
        {PRICE_BANDS.map((b, i) => (
          <option key={b.label} value={i}>{b.label}</option>
        ))}
      </select>

      <select
        aria-label="Brand"
        className={sel}
        value={params.get("brand") ?? ""}
        onChange={(e) => update({ brand: e.target.value })}
      >
        <option value="">All brands</option>
        {brands.map((b) => (
          <option key={b} value={b}>{b}</option>
        ))}
      </select>

      <select
        aria-label="Rating"
        className={sel}
        value={params.get("rating") ?? ""}
        onChange={(e) => update({ rating: e.target.value })}
      >
        <option value="">Any rating</option>
        <option value="4.5">4.5★ &amp; up</option>
        <option value="4">4★ &amp; up</option>
        <option value="3">3★ &amp; up</option>
      </select>

      <select
        aria-label="Sort"
        className={sel}
        value={params.get("sort") ?? ""}
        onChange={(e) => update({ sort: e.target.value })}
      >
        {SORT_OPTIONS.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>

      {active && (
        <button
          onClick={() => update({ min: "", max: "", brand: "", rating: "", sort: "" })}
          className="text-[13px] text-brand font-semibold px-2"
        >
          ✕ Clear filters
        </button>
      )}
    </div>
  );
}
