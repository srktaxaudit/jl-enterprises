/* ══════════════════════════════════════════════════════════════════════
   JL Enterprises — public storefront API client + shared product card.
   Read-only catalog calls to the backend (no auth needed for browsing).
   Envelope { success, message, data, timestamp }; helpers return `data`.
   Pair with script.js for the localStorage cart (event-delegated .addbtn).
   ══════════════════════════════════════════════════════════════════════ */

const JL_API_BASE = (() => {
  const h = location.hostname;
  if (h === "localhost" || h === "127.0.0.1") return "http://localhost:8081";
  return "https://jl-enterprises-api.onrender.com";   // Render backend
})();

async function jlPublicApi(path) {
  const res = await fetch(JL_API_BASE + path);
  let json = null;
  try { json = await res.json(); } catch (_) { /* empty */ }
  if (!res.ok || (json && json.success === false)) {
    throw { status: res.status, message: (json && json.message) || "Request failed" };
  }
  return json ? json.data : null;
}

const JL_CAT_EMOJI = {
  "air-conditioners": "❄️", "televisions": "📺", "refrigerators": "🧊",
  "washing-machines": "🌀", "home-theatre": "🔊", "kitchen": "🍳", "furniture": "🛋️",
};

const JLStore = {
  /** PageResponse of ProductSummaryDto. Optional search / category / featured filters. */
  products: (opts = {}) => {
    const p = new URLSearchParams({ size: String(opts.size || 40) });
    if (opts.search) p.set("search", opts.search);
    if (opts.category) p.set("category", opts.category);
    if (opts.featured) p.set("featured", "true");
    return jlPublicApi("/api/v1/products?" + p.toString());
  },
  productBySlug: (slug) => jlPublicApi("/api/v1/products/" + encodeURIComponent(slug)),
  categories: () => jlPublicApi("/api/v1/categories"),
};

// ── Shared helpers + product card (used by index.html and every category page) ──
function jlInr(n) { return "₹" + Math.round(Number(n || 0)).toLocaleString("en-IN"); }
function jlPctOff(mrp, price) { return (!mrp || mrp <= price) ? 0 : Math.round(((mrp - price) / mrp) * 100); }
function jlEsc(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

/** Render one storefront card from a ProductSummaryDto (API shape). */
function jlProductCard(p) {
  const price = Number(p.price || 0);
  const mrp = Number(p.comparePrice || 0);
  const off = jlPctOff(mrp, price);
  const rating = Math.round(Number(p.averageRating || 0));
  const emi = Math.max(1, Math.round(price / 21));
  const emoji = JL_CAT_EMOJI[p.categorySlug] || "📦";
  const searchText = jlEsc(((p.name || "") + " " + (p.brandName || "") + " " + (p.categorySlug || "")).toLowerCase());
  const media = p.primaryImageUrl
    ? `<img src="${jlEsc(p.primaryImageUrl)}" alt="${jlEsc(p.name)}" class="h-40 w-full object-cover" />`
    : `<div class="h-40 flex items-center justify-center text-6xl bg-gradient-to-br from-slate-50 to-slate-100">${emoji}</div>`;
  return `
    <div class="product-card bg-white border border-slate-200 rounded-2xl overflow-hidden hover:shadow-card hover:-translate-y-0.5 transition relative" data-search="${searchText}">
      ${off > 0 ? `<span class="absolute top-3 left-3 bg-red-600 text-white text-[11px] font-bold px-2 py-0.5 rounded z-[2]">-${off}%</span>` : ""}
      <a href="product.html?slug=${jlEsc(p.slug)}">${media}</a>
      <div class="p-3.5">
        <div class="text-[11px] text-slate-400 uppercase font-semibold tracking-wide">${jlEsc(p.brandName || "")}</div>
        <a href="product.html?slug=${jlEsc(p.slug)}" class="block text-[15px] text-navy font-semibold mt-0.5 mb-1.5 leading-snug h-9 overflow-hidden hover:text-brand">${jlEsc(p.name)}</a>
        <div class="text-amber text-[13px] mb-2">${"★".repeat(rating)}<span class="text-slate-400"> (${p.reviewCount || 0})</span></div>
        <div class="flex items-baseline gap-2 mb-0.5">
          <span class="text-xl font-extrabold text-navy">${jlInr(price)}</span>
          ${mrp > price ? `<span class="text-[13px] text-slate-400 line-through">${jlInr(mrp)}</span>` : ""}
        </div>
        <div class="text-[12px] text-brand mb-2.5">EMI from ${jlInr(emi)}/mo</div>
        <button data-id="${jlEsc(p.id)}" data-name="${jlEsc(p.name)}" data-brand="${jlEsc(p.brandName || "")}" data-emoji="${emoji}" data-price="${price}"
          class="addbtn w-full bg-navy hover:bg-orange text-white font-bold py-2.5 rounded-lg text-sm transition">🛒 Add to Cart</button>
      </div>
    </div>`;
}
