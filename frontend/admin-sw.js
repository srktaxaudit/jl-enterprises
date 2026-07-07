/* ══════════════════════════════════════════════════════════════════════
   JL Admin — minimal service worker (enables PWA install + offline shell).
   NETWORK-FIRST so the admin always shows live data when online; the cache
   is only a fallback when the phone is offline. API calls are never cached.
   ══════════════════════════════════════════════════════════════════════ */
const CACHE = "jl-admin-v1";
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
