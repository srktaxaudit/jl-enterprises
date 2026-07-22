/* ══════════════════════════════════════════════════════════════════════
   JL Admin — minimal service worker (enables PWA install + offline shell).
   NETWORK-FIRST so the admin always shows live data when online; the cache
   is only a fallback when the phone is offline. API calls are never cached.

   SCOPE WARNING: this file sits at the site root, so the browser gives it
   control over EVERY page on the domain — including the customer storefront.
   The fetch handler below must therefore ignore anything that is not an
   admin page or shared shell asset. Before v3 it intercepted every GET:
   it cached the whole storefront unboundedly and, offline, could serve the
   STAFF LOGIN PAGE to a shopper on any storefront URL.
   ══════════════════════════════════════════════════════════════════════ */
const CACHE = "jl-admin-v3";
const SHELL = [
  "/admin-login.html",
  "/admin-shell.html",
  "/config.js",
  "/admin.js",
  "/admin-shell.js",
  "/admin-shell.css",
  "/jl-ui.js",
  "/store.js",
  "/jl-admin-icon.svg",
];

/* Admin pages (admin.html, admin-login.html, admin-orders.html, …) plus the
   handful of shared shell assets the admin needs offline. Everything else —
   the entire customer storefront — is NOT ours to touch. */
const SHELL_SET = new Set(SHELL);
function isAdminRequest(pathname) {
  return /^\/admin[\w-]*\.html$/.test(pathname) || SHELL_SET.has(pathname);
}

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)).catch(() => {}));
  self.skipWaiting();
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))),
  );
  self.clients.claim();
});

self.addEventListener("fetch", (e) => {
  const req = e.request;
  if (req.method !== "GET") return;                     // never touch API writes
  let url;
  try { url = new URL(req.url); } catch (_) { return; }
  if (url.origin !== self.location.origin) return;      // don't cache the API (onrender) or other hosts
  if (!isAdminRequest(url.pathname)) return;            // storefront traffic passes through untouched
  e.respondWith(
    fetch(req)
      .then((res) => {
        const copy = res.clone();
        caches.open(CACHE).then((c) => c.put(req, copy)).catch(() => {});
        return res;
      })
      .catch(() => caches.match(req).then((r) => r || caches.match("/admin-login.html"))),
  );
});
