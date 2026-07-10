/* ══════════════════════════════════════════════════════════════════════
   JL Enterprises — single source for the backend API base URL.
   Loaded before store.js / admin.js on every page. If the API ever moves
   (e.g. https://api.jlstores.in), change it here once — nowhere else.
   ══════════════════════════════════════════════════════════════════════ */
window.JL_API_BASE = window.JL_API_BASE || "https://jl-enterprises-api.onrender.com";

// ── Site identity (SEO / sharing / analytics) ──────────────────────────────
// Public site origin — used for canonical URLs, Open Graph and the sitemap.
window.JL_SITE_URL = window.JL_SITE_URL || "https://jlstores.in";
// Default social-share image (Open Graph). 1200×630 recommended.
window.JL_OG_IMAGE = window.JL_OG_IMAGE || (window.JL_SITE_URL + "/og-image.svg");
// Google Analytics 4 Measurement ID, e.g. "G-XXXXXXXXXX". PASTE YOURS HERE to
// turn on analytics; leave blank to keep it off. (script.js reads this.)
window.JL_GA_ID = window.JL_GA_ID || "";
