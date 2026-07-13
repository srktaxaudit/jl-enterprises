/* ══════════════════════════════════════════════════════════════════════
   JL Enterprises — public storefront API client + shared product card.
   Read-only catalog calls to the backend (no auth needed for browsing).
   Envelope { success, message, data, timestamp }; helpers return `data`.
   Pair with script.js for the localStorage cart (event-delegated .addbtn).
   ══════════════════════════════════════════════════════════════════════ */

const JL_API_BASE = (() => {
  const h = location.hostname;
  if (h === "localhost" || h === "127.0.0.1") return "http://localhost:8081";
  return window.JL_API_BASE;   // single source: config.js (loaded first)
})();

const jlSleep = (ms) => new Promise((r) => setTimeout(r, ms));

/** Optional hook: a page can set window.JL_ON_WAKE to show a "server waking up"
    hint while a cold free-tier backend spins up between retries. Also updates the
    blocking overlay text (if one is showing) so the user knows why it's slow. */
function jlNotifyWaking() {
  if (typeof jlBusy !== "undefined") jlBusy.text("Waking up the server… this can take a moment.");
  if (typeof window.JL_ON_WAKE === "function") { try { window.JL_ON_WAKE(); } catch (_) { /* noop */ } }
}

// Backoff schedule that rides out a Render free-tier cold start (a Docker Spring
// Boot service can take 30–60s to wake). Total wait across all retries ≈ 37s.
const JL_RETRY_DELAYS = [2000, 4000, 6000, 10000, 15000];

/** Core JSON fetch with envelope-unwrap + cold-start retry.
    Only IDEMPOTENT reads (GET/HEAD) are auto-retried — re-sending a POST could
    duplicate the action (e.g. place the same order twice), so mutations get a single
    attempt and a clear "try again" message. App-level responses (incl. 4xx/500) are
    returned to the caller as-is. Set opts.auth to attach the customer bearer token. */
async function jlFetchJson(path, opts = {}) {
  const method = opts.method || (opts.body ? "POST" : "GET");
  // Any state-changing call (POST/PUT/PATCH/DELETE) shows the blocking overlay so
  // the user can't double-submit or interact mid-operation. Reads stay non-blocking
  // (pages use inline spinners for content). Pass opts.blocking:false to opt out.
  const blocking = method !== "GET" && opts.blocking !== false && typeof jlBusy !== "undefined";
  if (blocking) jlBusy.show(opts.busyMessage || "Please wait…");
  try {
    return await jlFetchJsonRaw(path, { ...opts, method });
  } finally {
    if (blocking) jlBusy.hide();
  }
}

async function jlFetchJsonRaw(path, opts) {
  const { method, body, auth } = opts;
  const verb = (method || (body ? "POST" : "GET")).toUpperCase();
  const retryable = verb === "GET" || verb === "HEAD";   // never auto-retry a mutation
  let lastErr = { status: 0, message: "Can't reach the server. Please check your connection and try again." };
  for (let i = 0; i <= JL_RETRY_DELAYS.length; i++) {
    let res;
    try {
      const tok = auth ? localStorage.getItem(JL_CTOK) : null;
      res = await fetch(JL_API_BASE + path, {
        method: verb,
        headers: {
          ...(body ? { "Content-Type": "application/json" } : {}),
          ...(tok ? { Authorization: "Bearer " + tok } : {}),
        },
        body: body ? JSON.stringify(body) : undefined,
      });
    } catch (netErr) {
      if (retryable && i < JL_RETRY_DELAYS.length) { jlNotifyWaking(); await jlSleep(JL_RETRY_DELAYS[i]); continue; }
      throw lastErr;
    }
    if ([502, 503, 504].includes(res.status)) {
      if (retryable && i < JL_RETRY_DELAYS.length) { jlNotifyWaking(); await jlSleep(JL_RETRY_DELAYS[i]); continue; }
      if (!retryable) throw { status: res.status, message: "The server is starting up — please try again in a moment." };
    }
    let json = null;
    try { json = await res.json(); } catch (_) { /* empty */ }
    if (res.status === 401 && auth) JLCustomer.logout();   // token expired/invalid
    if (!res.ok || (json && json.success === false)) {
      // On a 400 "Validation failed", the backend returns a {field:message} map in
      // `data` — carry it as `errors` so forms can show inline server-side messages.
      const errors = (json && json.data && typeof json.data === "object" && !Array.isArray(json.data)) ? json.data : undefined;
      throw { status: res.status, message: (json && json.message) || "Request failed", errors };
    }
    return json ? json.data : null;
  }
  throw lastErr;
}

/** Public (unauthenticated) catalog call. */
function jlPublicApi(path) { return jlFetchJson(path, { auth: false }); }

const JL_CAT_EMOJI = {
  "air-conditioners": "❄️", "televisions": "📺", "refrigerators": "🧊",
  "washing-machines": "🌀", "home-theatre": "🔊", "kitchen": "🍳", "furniture": "🛋️",
};

/* ── Customer authentication (storefront) ─────────────────────────────
   Separate token store from the admin panel (admin.js uses jl_access) so a
   customer session never collides with a staff session. Bearer tokens only
   — no cookies — matching the backend CORS config (allowCredentials=false). */
const JL_CTOK = "jl_cust_access", JL_CREFRESH = "jl_cust_refresh", JL_CUSER = "jl_cust_user";

// Roles that grant admin-panel access. Such accounts must sign in at the admin login,
// not the storefront — mirrors admin.js's STAFF_ROLES so the two logins stay complementary.
const JL_STAFF_ROLES = [
  "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_MANAGER",
  "ROLE_INVENTORY_MANAGER", "ROLE_ORDER_MANAGER", "ROLE_PRODUCT_MANAGER",
  "ROLE_MARKETING_MANAGER", "ROLE_CUSTOMER_SUPPORT", "ROLE_ACCOUNTANT",
];
function jlUserIsStaff(user) {
  return !!user && (user.roles || []).some((r) => JL_STAFF_ROLES.includes(r));
}

/** Authenticated JSON call (customer bearer token). Shares the retry/backoff in
    jlFetchJson so auth flows also survive a free-tier cold start. */
function jlAuthApi(path, body, method) {
  return jlFetchJson(path, { method, body, auth: true });
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
  async register({ email, password, firstName, lastName, phone, whatsappOptIn }) {
    const d = await jlAuthApi("/api/v1/auth/register", { email, password, firstName, lastName, phone, whatsappOptIn: !!whatsappOptIn });
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

/* ── Wishlist (customer) — backed by /api/v1/wishlist ── */
const JLWishlist = {
  get: () => jlAuthApi("/api/v1/wishlist", null, "GET"),
  add: (productId) => jlAuthApi("/api/v1/wishlist/items/" + productId, {}, "POST"),
  remove: (productId) => jlAuthApi("/api/v1/wishlist/items/" + productId, null, "DELETE"),
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
  /** Place an order from the current backend cart. method = "COD" | "RAZORPAY". Returns OrderDto. */
  placeOrder(shippingAddressId, notes, method, couponCode, exchangeRequestId) {
    return jlAuthApi("/api/v1/orders",
      { shippingAddressId, paymentMethod: method || "COD", notes: notes || "",
        couponCode: couponCode || null, exchangeRequestId: exchangeRequestId || null });
  },
  /** Active coupons (public — for browsing/offers page, no per-user filtering). */
  activeCoupons: () => jlPublicApi("/api/v1/coupons/active"),
  /** Coupons applicable to the authenticated customer's persisted cart. */
  eligibleCoupons() { return jlAuthApi("/api/v1/coupons/eligible"); },
  /** Validation derives products and prices from the server-side cart. */
  validateCoupon(code) {
    return jlAuthApi("/api/v1/coupons/validate?code=" + encodeURIComponent(code));
  },
  /** Start an online payment. Returns PaymentInitResponse
      { providerReference (razorpay order id), clientData (key id), amount, currency, ... }. */
  initiatePayment(orderId) {
    return jlAuthApi("/api/v1/payments/" + encodeURIComponent(orderId) + "/initiate", {}, "POST");
  },
  /** Confirm an online payment after the provider callback. */
  confirmPayment(orderId, body) {
    return jlAuthApi("/api/v1/payments/" + encodeURIComponent(orderId) + "/confirm", body);
  },
  myOrders: () => jlAuthApi("/api/v1/orders?size=50&sort=placedAt,desc"),
  order: (id) => jlAuthApi("/api/v1/orders/" + encodeURIComponent(id)),
  cancelOrder: (id, reason) => jlAuthApi("/api/v1/orders/" + encodeURIComponent(id) + "/cancel?reason=" + encodeURIComponent(reason || ""), {}, "POST"),
  requestReturn: (id, reason) => jlAuthApi("/api/v1/orders/" + encodeURIComponent(id) + "/return?reason=" + encodeURIComponent(reason || ""), {}, "POST"),
  invoice: (id) => jlAuthApi("/api/v1/orders/" + encodeURIComponent(id) + "/invoice"),
};

/* ── Exchange (trade-in) helpers (customer, authenticated) ─────────────── */
const JLExchange = {
  create: (body) => jlAuthApi("/api/v1/exchanges", body),
  mine: () => jlAuthApi("/api/v1/exchanges/mine"),
  /** Approved, unused exchanges the customer can apply at checkout. */
  checkoutOptions: () => jlAuthApi("/api/v1/exchanges/checkout-options"),
  /** Upload one appliance photo (multipart). Returns { url }. */
  async uploadImage(file) {
    const fd = new FormData();
    fd.append("file", file);
    const tok = localStorage.getItem(JL_CTOK);
    const res = await fetch(JL_API_BASE + "/api/v1/exchanges/images", {
      method: "POST",
      headers: tok ? { Authorization: "Bearer " + tok } : {},
      body: fd,
    });
    let json = null;
    try { json = await res.json(); } catch (_) { /* empty */ }
    if (!res.ok || (json && json.success === false)) {
      throw { status: res.status, message: (json && json.message) || "Upload failed" };
    }
    return json ? json.data : null;
  },
};

const JLStore = {
  /** PageResponse of ProductSummaryDto. Optional search / category / featured filters. */
  products: (opts = {}) => {
    const p = new URLSearchParams({ size: String(opts.size || 40) });
    if (opts.page) p.set("page", String(opts.page));
    if (opts.search) p.set("search", opts.search);
    if (opts.category) p.set("category", opts.category);
    if (opts.featured) p.set("featured", "true");
    return jlPublicApi("/api/v1/products?" + p.toString());
  },
  productBySlug: (slug) => jlPublicApi("/api/v1/products/" + encodeURIComponent(slug)),
  categories: () => jlPublicApi("/api/v1/categories"),
  banners: (position) => jlPublicApi("/api/v1/banners" + (position ? "?position=" + encodeURIComponent(position) : "")),
};

// ── Shared helpers + product card (used by index.html and every category page) ──
function jlInr(n) { return "₹" + Math.round(Number(n || 0)).toLocaleString("en-IN"); }
function jlPctOff(mrp, price) { return (!mrp || mrp <= price) ? 0 : Math.round(((mrp - price) / mrp) * 100); }
function jlEsc(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

/** Closing-stock status line for a card. Mirrors product.html's states:
 *  0 → red "Out of stock", 1–5 → orange "Only X left", else green "In stock".
 *  Returns "" when the API didn't send availableStock (e.g. wishlist rows). */
function jlStockLine(stock) {
  if (stock == null) return "";
  if (stock <= 0) return `<div class="text-[12px] font-bold text-red-600 mb-1.5">Out of stock</div>`;
  if (stock <= 5) return `<div class="text-[12px] font-semibold text-orange-600 mb-1.5">Only ${stock} left in stock</div>`;
  return `<div class="text-[12px] font-semibold text-emerald-600 mb-1.5">✔ In stock</div>`;
}

/** Card footer: the cart control, or a disabled button when closing stock is 0. */
function jlCardCartOrOOS(p, emoji, stock) {
  if (stock != null && stock <= 0) {
    return `<button disabled class="w-full bg-slate-300 text-white font-bold py-2.5 rounded-lg text-sm cursor-not-allowed">❌ Out of Stock</button>`;
  }
  return jlCartControl(
    { id: p.id, name: p.name, brand: p.brandName || "", emoji, price: Number(p.price || 0), stock: (stock == null ? "" : stock) },
    { variant: "card" }
  );
}

/** Render one storefront card from a ProductSummaryDto (API shape). */
function jlProductCard(p) {
  const price = Number(p.price || 0);
  const mrp = Number(p.comparePrice || 0);
  const off = jlPctOff(mrp, price);
  const rating = Math.round(Number(p.averageRating || 0));
  const emoji = JL_CAT_EMOJI[p.categorySlug] || "📦";
  const searchText = jlEsc(((p.name || "") + " " + (p.brandName || "") + " " + (p.categorySlug || "")).toLowerCase());
  const media = p.primaryImageUrl
    ? `<img src="${jlEsc(p.primaryImageUrl)}" alt="${jlEsc(p.name)}" loading="lazy" decoding="async" class="h-40 w-full object-cover" />`
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
        ${(p.emiAvailable && p.emiAmount) ? `<div class="text-[12px] text-brand mb-1.5">EMI ${jlInr(p.emiAmount)}/mo${p.emiMonths ? ` for ${p.emiMonths} months` : ""}</div>` : `<div class="mb-1.5"></div>`}
        ${jlStockLine(p.availableStock == null ? null : Number(p.availableStock))}
        ${jlCardCartOrOOS(p, emoji, p.availableStock == null ? null : Number(p.availableStock))}
      </div>
    </div>`;
}

/* ── Recently viewed products (localStorage; purely client-side) ────────── */
const JL_RV_KEY = "jl_recently_viewed";
function jlPushRecentlyViewed(item) {
  if (!item || !item.slug) return;
  try {
    let list = JSON.parse(localStorage.getItem(JL_RV_KEY)) || [];
    list = list.filter((x) => x && x.slug !== item.slug);
    list.unshift({ slug: item.slug, name: item.name || "", price: Number(item.price) || 0,
                   image: item.image || "", brand: item.brand || "" });
    localStorage.setItem(JL_RV_KEY, JSON.stringify(list.slice(0, 12)));
  } catch (_) { /* storage disabled/full */ }
}
function jlGetRecentlyViewed(excludeSlug) {
  try {
    const list = JSON.parse(localStorage.getItem(JL_RV_KEY)) || [];
    return excludeSlug ? list.filter((x) => x && x.slug !== excludeSlug) : list;
  } catch (_) { return []; }
}
/** Render a horizontal "Recently viewed" rail into #containerId. No-op (hidden) when empty. */
function jlRenderRecentlyViewed(containerId, opts) {
  opts = opts || {};
  const box = document.getElementById(containerId);
  if (!box) return;
  const items = jlGetRecentlyViewed(opts.exclude).slice(0, opts.limit || 12);
  if (!items.length) { box.style.display = "none"; return; }
  const esc = (s) => String(s == null ? "" : s).replace(/[&<>"']/g,
    (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  const card = (x) => `
    <a href="product.html?slug=${encodeURIComponent(x.slug)}" class="group flex-shrink-0 w-40 bg-white border border-slate-200 rounded-2xl p-3 hover:shadow-card hover:-translate-y-0.5 transition">
      <div class="h-28 rounded-xl overflow-hidden bg-slate-50 flex items-center justify-center mb-2">
        ${x.image ? `<img src="${esc(x.image)}" alt="${esc(x.name)}" loading="lazy" class="w-full h-full object-cover" />` : `<span class="text-4xl">📦</span>`}
      </div>
      ${x.brand ? `<div class="text-[11px] text-slate-400 truncate">${esc(x.brand)}</div>` : ""}
      <div class="text-[13px] font-semibold text-navy leading-snug mb-1" style="display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;min-height:34px">${esc(x.name)}</div>
      <div class="text-navy font-extrabold">${jlInr(x.price)}</div>
    </a>`;
  box.innerHTML = `
    <div class="flex items-center justify-between mb-3">
      <h2 class="text-xl font-bold text-navy">${esc(opts.title || "Recently viewed")}</h2>
      <button type="button" data-rv-clear class="text-[12px] text-slate-400 hover:text-red-500">Clear</button>
    </div>
    <div class="flex gap-3 overflow-x-auto pb-2">${items.map(card).join("")}</div>`;
  box.style.display = "";
  const clr = box.querySelector("[data-rv-clear]");
  if (clr) clr.addEventListener("click", () => { try { localStorage.removeItem(JL_RV_KEY); } catch (_) {} box.style.display = "none"; });
}
window.jlPushRecentlyViewed = jlPushRecentlyViewed;
window.jlGetRecentlyViewed = jlGetRecentlyViewed;
window.jlRenderRecentlyViewed = jlRenderRecentlyViewed;

/* ── Header login state ────────────────────────────────────────────────
   Storefront headers are static (they always show Sign Up / Login). When a
   customer is logged in, swap those for "My Orders" + "Logout" so they can
   reach their orders and sign out. Runs on every page that loads store.js.
   Only touches links INSIDE <header> — footer "Login/Sign Up" links are left
   as-is. Handles both header variants: full (Sign Up + Login) and compact
   (Login only, on category pages). */
function jlLogoutAndGo(e) { if (e) e.preventDefault(); JLCustomer.logout(); location.href = "index.html"; }

function jlRenderAuthNav() {
  const header = document.querySelector("header");
  if (!header || !JLCustomer.isLoggedIn()) return;
  const nav = header.querySelector("nav");
  const login = header.querySelector('a[href="login.html"]');
  const signup = header.querySelector('a[href="signup.html"]');
  const hasOrders = header.querySelector('a[href="my-orders.html"]');
  const linkHTML = (emoji, label) => `<span class="text-xl">${emoji}</span>${label}`;

  // Add an "Account" link for logged-in users (once), before the Cart link.
  if (nav && !header.querySelector('a[href="account.html"]')) {
    const acc = document.createElement("a");
    acc.className = "flex flex-col items-center text-[11px]";
    acc.setAttribute("href", "account.html");
    acc.innerHTML = linkHTML("👤", "Account");
    const cart = nav.querySelector('a[href="cart.html"]');
    if (cart) nav.insertBefore(acc, cart); else nav.appendChild(acc);
  }

  if (signup) {                       // full header → signup becomes "My Orders"
    signup.setAttribute("href", "my-orders.html");
    signup.innerHTML = linkHTML("📦", "My Orders");
  }
  if (login) {
    if (signup) {                     // …and login becomes "Logout"
      login.setAttribute("href", "#");
      login.innerHTML = linkHTML("🚪", "Logout");
      login.addEventListener("click", jlLogoutAndGo);
    } else {                          // compact header (Login only): repurpose + add Logout
      login.setAttribute("href", "my-orders.html");
      login.innerHTML = linkHTML("📦", "My Orders");
      const out = login.cloneNode(true);
      out.setAttribute("href", "#");
      out.innerHTML = linkHTML("🚪", "Logout");
      out.addEventListener("click", jlLogoutAndGo);
      login.parentNode.insertBefore(out, login.nextSibling);
    }
    return;
  }
  // No login/signup links (e.g. product.html) and no existing orders link:
  // inject "My Orders" + "Logout" into the header nav, before the Cart link.
  if (nav && !hasOrders) {
    const cls = "flex flex-col items-center text-[11px]";
    const cart = nav.querySelector('a[href="cart.html"]');
    const mk = (emoji, label, href, onClick) => {
      const a = document.createElement("a");
      a.className = cls; a.setAttribute("href", href); a.innerHTML = linkHTML(emoji, label);
      if (onClick) a.addEventListener("click", onClick);
      return a;
    };
    const orders = mk("📦", "My Orders", "my-orders.html");
    const out = mk("🚪", "Logout", "#", jlLogoutAndGo);
    if (cart) { nav.insertBefore(orders, cart); nav.insertBefore(out, cart); }
    else { nav.appendChild(orders); nav.appendChild(out); }
  }
}

/** Make the header's icon links (Service/Sign Up/Login/My Orders/Logout) visible on
    mobile too — the markup hides them with `hidden sm:flex`. Cart is already shown. */
function jlMobileNav() {
  document.querySelectorAll("header nav a.hidden").forEach((a) => {
    if (a.classList.contains("sm:flex")) { a.classList.remove("hidden"); a.classList.add("flex"); }
  });
}

/** Fetch the admin-managed site logo (public endpoint) and apply it everywhere:
    the "JL" logo boxes in the header, and the browser favicon. Best-effort — if no
    logo is configured or the call fails, the default "JL" branding stays. */
async function jlApplyBranding() {
  try {
    const res = await fetch(JL_API_BASE + "/api/v1/branding");
    if (!res.ok) return;
    const json = await res.json();
    const url = json && json.data && json.data.logoUrl;
    if (url) jlSetLogo(url);
  } catch (_) { /* branding is optional */ }
}

/** Replace every leaf "JL" logo box with the logo image, and point the favicon
    at it. Matches only elements whose text is exactly "JL" (the wordmark/footer
    read "JL Enterprises"), so it works across all pages without per-page markup. */
function jlSetLogo(url) {
  document.querySelectorAll("body *").forEach((el) => {
    if (el.children.length === 0 && el.textContent.trim() === "JL") {
      el.textContent = "";
      el.style.background = "none";
      el.style.padding = "0";
      const img = document.createElement("img");
      img.src = url; img.alt = "JL Enterprises";
      img.style.height = "100%"; img.style.width = "auto";
      img.style.maxWidth = "130px"; img.style.objectFit = "contain";
      el.appendChild(img);
    }
  });
  let link = document.querySelector('link[rel="icon"]');
  if (!link) { link = document.createElement("link"); link.rel = "icon"; document.head.appendChild(link); }
  link.href = url;
}

document.addEventListener("DOMContentLoaded", () => { jlRenderAuthNav(); jlMobileNav(); jlApplyBranding(); });
