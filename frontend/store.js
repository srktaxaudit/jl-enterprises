/* ══════════════════════════════════════════════════════════════════════
   JL Enterprises — public storefront API client.
   Read-only catalog calls to the backend (no auth needed for browsing).
   Response envelope is { success, message, data, timestamp }; helpers below
   return the `data` payload. Pair with script.js for the localStorage cart.
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

/** Emoji fallback per category slug (used when a product has no image). */
const JL_CAT_EMOJI = {
  "air-conditioners": "❄️", "televisions": "📺", "refrigerators": "🧊",
  "washing-machines": "🌀", "home-theatre": "🔊", "kitchen": "🍳", "furniture": "🛋️",
};

const JLStore = {
  /** PageResponse of ProductSummaryDto. Optional search / category filters. */
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
