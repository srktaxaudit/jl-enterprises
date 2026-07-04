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

/* ── Customer authentication (storefront) ─────────────────────────────
   Separate token store from the admin panel (admin.js uses jl_access) so a
   customer session never collides with a staff session. Bearer tokens only
   — no cookies — matching the backend CORS config (allowCredentials=false). */
const JL_CTOK = "jl_cust_access", JL_CREFRESH = "jl_cust_refresh", JL_CUSER = "jl_cust_user";

/** Authenticated JSON call. Sends the customer bearer token when present.
    Used for both auth (no token yet) and authed calls; unwraps the envelope. */
async function jlAuthApi(path, body, method) {
  const tok = localStorage.getItem(JL_CTOK);
  const res = await fetch(JL_API_BASE + path, {
    method: method || (body ? "POST" : "GET"),
    headers: {
      "Content-Type": "application/json",
      ...(tok ? { Authorization: "Bearer " + tok } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  let json = null;
  try { json = await res.json(); } catch (_) { /* empty */ }
  if (res.status === 401) JLCustomer.logout();   // token expired/invalid
  if (!res.ok || (json && json.success === false)) {
    throw { status: res.status, message: (json && json.message) || "Request failed" };
  }
  return json ? json.data : null;
}

const JLCustomer = {
  token: () => localStorage.getItem(JL_CTOK),
  isLoggedIn: () => !!localStorage.getItem(JL_CTOK),
  user: () => { try { return JSON.parse(localStorage.getItem(JL_CUSER) || "null"); } catch { return null; } },
  _save(auth) {
    if (!auth || !auth.accessToken) return;
    localStorage.setItem(JL_CTOK, auth.accessToken);
    if (auth.refreshToken) localStorage.setItem(JL_CREFRESH, auth.refreshToken);
    localStorage.setItem(JL_CUSER, JSON.stringify(auth.user || {}));
  },
  async register({ email, password, firstName, lastName, phone }) {
    const d = await jlAuthApi("/api/v1/auth/register", { email, password, firstName, lastName, phone });
    this._save(d); return d;
  },
  async login(email, password, rememberMe) {
    const d = await jlAuthApi("/api/v1/auth/login", { email, password, rememberMe: !!rememberMe });
    this._save(d); return d;
  },
  logout() {
    localStorage.removeItem(JL_CTOK);
    localStorage.removeItem(JL_CREFRESH);
    localStorage.removeItem(JL_CUSER);
  },
};

/* ── Checkout helpers (place a real order from the localStorage cart) ──── */
const JLCheckout = {
  /** Mirror the localStorage cart into the backend cart. Clears any stale
      backend lines first, then adds each item. Returns {added, failed:[{name,reason}]}. */
  async syncCart(cart) {
    await jlAuthApi("/api/v1/cart", null, "DELETE").catch(() => { /* empty cart is fine */ });
    let added = 0; const failed = [];
    for (const it of cart) {
      try {
        await jlAuthApi("/api/v1/cart/items", { productId: it.id, quantity: it.qty });
        added++;
      } catch (e) {
        failed.push({ name: it.name || it.id, reason: e.message || "could not be added" });
      }
    }
    return { added, failed };
  },
  /** Create a shipping address (type BOTH covers shipping + billing). Returns AddressDto. */
  addAddress(a) {
    return jlAuthApi("/api/v1/addresses", {
      type: "BOTH",
      fullName: a.fullName, phone: a.phone,
      line1: a.line1, line2: a.line2 || "",
      city: a.city, state: a.state || "",
      postalCode: a.postalCode, country: a.country || "India",
      defaultAddress: true,
    });
  },
  /** Place a Cash-on-Delivery order from the current backend cart. Returns OrderDto. */
  placeOrder(shippingAddressId, notes) {
    return jlAuthApi("/api/v1/orders", { shippingAddressId, paymentMethod: "COD", notes: notes || "" });
  },
  myOrders: () => jlAuthApi("/api/v1/orders?size=50&sort=placedAt,desc"),
  order: (id) => jlAuthApi("/api/v1/orders/" + encodeURIComponent(id)),
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
