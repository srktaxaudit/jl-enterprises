/* ══════════════════════════════════════════════════════════════════════
   JL Enterprises — Admin frontend, wired to the ecommerce-backend API.
   Bearer-token auth against /api/v1/auth. Responses use the standard envelope
   { success, message, data, timestamp } — jlApi() returns the `data` payload.

   NOTE: tokens live in localStorage for simplicity. That is convenient but
   XSS-exposed; for higher security move to httpOnly cookies + a token endpoint.
   ══════════════════════════════════════════════════════════════════════ */

// Some Android WebViews / "desktop site" modes ignore the <meta viewport> and
// lay the page out at ~980px, so phones get a shrunken desktop admin. Detect
// the mismatch (physical screen far narrower than the layout) and re-assert
// the meta — WebViews re-parse a programmatic content change — which snaps the
// layout back to device width and lets the mobile drawer CSS apply.
(function jlFixViewport() {
  try {
    if (screen.width < 768 && window.innerWidth > 820) {
      const m = document.querySelector('meta[name="viewport"]');
      if (m) m.setAttribute("content", "width=device-width, initial-scale=1.0");
    }
  } catch (_) { /* best-effort */ }
})();

// Give the small row-action buttons (Edit / Hide / Delete / View / toggle) a
// bigger, rounded, easy-to-tap hit area with subtle hover + press feedback.
// Injected once here so every admin list page benefits without per-page edits.
(function jlStyleActionButtons() {
  const css =
    "button[data-edit],button[data-del],button[data-toggle],button[data-view]{" +
      "display:inline-flex;align-items:center;justify-content:center;" +
      "min-height:30px;padding:4px 11px;border-radius:8px;line-height:1.15;" +
      "transition:background-color .15s ease,transform .08s ease;}" +
    "button[data-edit]:hover,button[data-view]:hover{background:rgba(87,108,188,.12);}" +
    "button[data-toggle]:hover{background:rgba(100,116,139,.14);}" +
    "button[data-del]:hover{background:rgba(239,68,68,.12);}" +
    "button[data-edit]:active,button[data-del]:active,button[data-toggle]:active,button[data-view]:active{transform:scale(.93);}" +
    "button[data-edit]:focus-visible,button[data-del]:focus-visible,button[data-toggle]:focus-visible,button[data-view]:focus-visible{outline:2px solid #576cbc;outline-offset:2px;}";
  const s = document.createElement("style");
  s.setAttribute("data-jl", "action-buttons");
  s.textContent = css;
  (document.head || document.documentElement).appendChild(s);
})();

// Admin pages are rendered inside one persistent dashboard shell.  Keep direct
// bookmarks working by promoting standalone pages into that shell; pages loaded
// by the shell's iframe are left alone.
(function jlEnterAdminShell() {
  const page = location.pathname.split("/").pop() || "admin.html";
  if (window.top === window.self && page !== "admin-login.html" && page !== "admin-shell.html") {
    const target = page + location.search + location.hash;
    location.replace("admin-shell.html?page=" + encodeURIComponent(target));
  }
})();

// Where the Spring Boot API lives. Single source: config.js (loaded first).
// The backend runs only on Render, so admin always calls it (even from a local server).
const JL_API_BASE = window.JL_API_BASE;

const JL_LOGIN_PAGE = "admin-login.html";
const ACCESS_KEY = "jl_access";
const REFRESH_KEY = "jl_refresh";
const STAFF_ROLES = [
  "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_MANAGER",
  "ROLE_INVENTORY_MANAGER", "ROLE_ORDER_MANAGER", "ROLE_PRODUCT_MANAGER",
  "ROLE_MARKETING_MANAGER", "ROLE_CUSTOMER_SUPPORT", "ROLE_ACCOUNTANT",
];
// Full-access roles that can see/do everything in the admin.
const SUPER_ROLES = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN"];

// ── Persistent-shell guard ──────────────────────────────────────────────
// Every admin section runs inside admin-shell.html, whose sidebar never
// reloads. If an admin content page is opened on its OWN (a bookmark, an old
// link, or a redirect), bounce it into the shell so the sidebar is always
// present. Skipped when already embedded (top !== self) or on the shell/login
// pages themselves. Runs as early as admin.js loads to minimise any flash.
(function jlEnsureShell() {
  try {
    if (window.top !== window.self) return;                 // already inside the shell iframe
    const file = (location.pathname.split("/").pop() || "").toLowerCase();
    if (file === "admin-shell.html" || file === "admin-login.html") return;
    if (!/^admin(?:-[\w-]+)?\.html$/.test(file)) return;    // not an admin content page
    location.replace("admin-shell.html?page=" + encodeURIComponent(file + location.search + location.hash));
  } catch (_) { /* if anything is odd, just render the page standalone */ }
})();

/** The top-level window (the shell when embedded, else this window). Used for
    navigation/redirects so an auth failure inside the iframe moves the whole app. */
function jlTopWindow() { return window.top || window; }

/** The shell URL that represents whatever section is currently showing, so it can
    be preserved across a login redirect. */
function jlCurrentShellUrl() {
  const file = location.pathname.split("/").pop() || "admin.html";
  if (file === "admin-shell.html") return file + location.search;
  return "admin-shell.html?page=" + encodeURIComponent(file + location.search + location.hash);
}

// ── token storage ──
const jlTokens = {
  get access() { return localStorage.getItem(ACCESS_KEY); },
  get refresh() { return localStorage.getItem(REFRESH_KEY); },
  set(access, refresh) {
    if (access) localStorage.setItem(ACCESS_KEY, access);
    if (refresh) localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear() { localStorage.removeItem(ACCESS_KEY); localStorage.removeItem(REFRESH_KEY); },
};

// ── cold-start resilience ──
// Render's free tier sleeps when idle and takes 30–60s to wake; the first call
// then rejects ("Failed to fetch") or returns a 502/503/504 gateway error. These
// never reached the app, so retrying with backoff is safe even for POST /login.
const jlSleep = (ms) => new Promise((r) => setTimeout(r, ms));
const JL_RETRY_DELAYS = [2000, 4000, 6000, 10000, 15000];   // ≈37s total
function jlNotifyWaking() {
  if (typeof jlBusy !== "undefined") jlBusy.text("Waking up the server… this can take a moment.");
  if (typeof window.JL_ON_WAKE === "function") { try { window.JL_ON_WAKE(); } catch (_) { /* noop */ } }
}

async function jlFetchWithRetry(url, opts) {
  // Only AUTO-RETRY idempotent reads. Re-sending a POST/PUT/PATCH/DELETE on a gateway
  // hiccup can duplicate the action (e.g. create the same product twice), so mutations
  // get a single attempt and a clear "try again" message instead.
  const method = ((opts && opts.method) || "GET").toUpperCase();
  const retryable = method === "GET" || method === "HEAD";
  for (let i = 0; i <= JL_RETRY_DELAYS.length; i++) {
    try {
      const res = await fetch(url, opts);
      if ([502, 503, 504].includes(res.status)) {
        if (retryable && i < JL_RETRY_DELAYS.length) { jlNotifyWaking(); await jlSleep(JL_RETRY_DELAYS[i]); continue; }
        if (!retryable) throw { status: res.status, message: "The server is starting up — please try again in a moment." };
      }
      return res;
    } catch (e) {
      if (e && e.status) throw e;   // our own "starting up" signal — don't retry
      if (retryable && i < JL_RETRY_DELAYS.length) { jlNotifyWaking(); await jlSleep(JL_RETRY_DELAYS[i]); continue; }
      throw { status: 0, message: "Can't reach the server. It may be starting up — please wait a moment and try again." };
    }
  }
}

/**
 * fetch() wrapper. Adds the bearer token, unwraps the ApiResponse envelope, and
 * on a 401 tries a single token refresh before giving up. Returns `data`.
 * Throws { status, message } on failure.
 */
async function jlApi(path, { method = "GET", body, auth = true, blocking, busyMessage, _retried = false } = {}) {
  // The page overlay is OPT-IN (pass blocking:true) — not automatic per call. Initial
  // page loads wrap all their fetches in one jlWithBusy() so a single overlay covers
  // them; background ops (search, filter, pagination, create/update/delete) use local
  // button/table loaders instead of blocking the whole page. Never blocks the internal
  // 401→refresh retry; jlBusy is reference-counted + debounced so bursts share one overlay.
  const block = blocking === true && !_retried && typeof jlBusy !== "undefined";
  if (block) jlBusy.show(busyMessage || "Please wait…");
  try {
    const headers = {};
    const opts = { method, headers };
    if (body !== undefined) {
      headers["Content-Type"] = "application/json";
      opts.body = JSON.stringify(body);
    }
    if (auth && jlTokens.access) headers["Authorization"] = "Bearer " + jlTokens.access;

    const res = await jlFetchWithRetry(JL_API_BASE + path, opts);

    if (res.status === 401 && auth && !_retried && jlTokens.refresh) {
      if (await jlTryRefresh()) {
        return await jlApi(path, { method, body, auth, blocking: false, _retried: true });
      }
    }

    let json = null;
    try { json = await res.json(); } catch (_) { /* empty body */ }

    if (!res.ok || (json && json.success === false)) {
      const errors = (json && json.data && typeof json.data === "object" && !Array.isArray(json.data)) ? json.data : undefined;
      throw { status: res.status, message: (json && json.message) || "Request failed", errors };
    }
    return json ? json.data : null;
  } finally {
    if (block) jlBusy.hide();
  }
}

async function jlTryRefresh() {
  try {
    const res = await fetch(JL_API_BASE + "/api/v1/auth/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: jlTokens.refresh }),
    });
    if (!res.ok) return false;
    const json = await res.json();
    jlTokens.set(json.data.accessToken, json.data.refreshToken);
    return true;
  } catch (_) {
    return false;
  }
}

/**
 * Multipart file upload (FormData). Like jlApi but WITHOUT a JSON content-type
 * (the browser sets the multipart boundary). Rides the same cold-start retry and
 * 401→refresh handling. Returns the unwrapped `data`.
 */
async function jlUpload(path, file, _retried = false) {
  const block = !_retried && typeof jlBusy !== "undefined";
  if (block) jlBusy.show("Uploading…");
  try {
    const fd = new FormData();
    fd.append("file", file);
    const headers = {};
    if (jlTokens.access) headers["Authorization"] = "Bearer " + jlTokens.access;

    const res = await jlFetchWithRetry(JL_API_BASE + path, { method: "POST", headers, body: fd });

    if (res.status === 401 && !_retried && jlTokens.refresh) {
      if (await jlTryRefresh()) return await jlUpload(path, file, true);
    }
    let json = null;
    try { json = await res.json(); } catch (_) { /* empty */ }
    if (!res.ok || (json && json.success === false)) {
      throw { status: res.status, message: (json && json.message) || "Upload failed" };
    }
    return json ? json.data : null;
  } finally {
    if (block) jlBusy.hide();
  }
}

// ── auth calls ──
const JLAuth = {
  async login(email, password) {
    const data = await jlApi("/api/v1/auth/login", {
      method: "POST", auth: false, body: { email, password },
    });
    jlTokens.set(data.accessToken, data.refreshToken);
    return data.user;
  },
  me: () => jlApi("/api/v1/auth/me"),
  async logout() {
    try {
      if (jlTokens.refresh) {
        await jlApi("/api/v1/auth/logout", { method: "POST", auth: false, body: { refreshToken: jlTokens.refresh } });
      }
    } finally {
      jlTokens.clear();
    }
  },
};

/** Blocking notice via the UI toolkit, with a plain-alert fallback if it's absent. */
function jlNotify(msg, opts) {
  return (typeof jlAlert === "function") ? jlAlert(msg, opts) : Promise.resolve(window.alert(msg));
}

/** Human-friendly display name from a UserDto. */
function jlDisplayName(user) {
  const full = [user.firstName, user.lastName].filter(Boolean).join(" ").trim();
  return full || user.email;
}

function jlIsStaff(user) {
  return (user.roles || []).some((r) => STAFF_ROLES.includes(r));
}

/** True if the user is a full-access admin (SUPER_ADMIN/ADMIN). */
function jlIsSuper(user) {
  return (user.roles || []).some((r) => SUPER_ROLES.includes(r));
}

/** True if the user is a super role OR holds any of the given role names (bare, e.g. "ORDER_MANAGER"). */
function jlHasRole(user, ...roleNames) {
  if (jlIsSuper(user)) return true;
  const have = user.roles || [];
  return roleNames.some((rn) => have.includes("ROLE_" + rn) || have.includes(rn));
}

/**
 * Guard a protected admin page. Redirects to login if unauthenticated, or back
 * to the store if the user isn't staff. If `allowedRoles` is given, staff who are
 * not super-admins must hold at least one of them (else bounced to the dashboard).
 * Resolves with the current UserDto.
 */
async function jlRequireAdmin(allowedRoles) {
  let user;
  try {
    user = await JLAuth.me();
  } catch (_) {
    // Preserve the current shell section across login (redirect the top window so
    // an expiry inside the iframe moves the whole app, not just the frame).
    const back = encodeURIComponent(jlCurrentShellUrl());
    jlTopWindow().location.replace(JL_LOGIN_PAGE + "?next=" + back);
    return new Promise(() => {});
  }
  if (!jlIsStaff(user)) {
    await jlNotify("This account does not have staff access.", { title: "Access denied", type: "error" });
    jlTopWindow().location.replace("index.html");
    return new Promise(() => {});
  }
  // MANAGER has broad operational access; super-admins always pass.
  if (allowedRoles && allowedRoles.length && !jlIsSuper(user) && !jlHasRole(user, "MANAGER", ...allowedRoles)) {
    await jlNotify("You don't have access to this section.", { title: "Access denied", type: "warn" });
    jlTopWindow().location.replace("admin-shell.html");
    return new Promise(() => {});
  }
  return user;
}

/** Guard an admin-only page (SUPER_ADMIN / ADMIN only) — e.g. Staff, Settings. */
async function jlRequireSuper() {
  const user = await jlRequireAdmin();
  if (!jlIsSuper(user)) {
    await jlNotify("This section is restricted to administrators.", { title: "Restricted", type: "warn" });
    jlTopWindow().location.replace("admin-shell.html");
    return new Promise(() => {});
  }
  return user;
}

/** Wire a logout control: clears the session then returns to the login page. */
function jlWireLogout(selector) {
  const el = document.querySelector(selector);
  if (!el) return;
  el.addEventListener("click", async (ev) => {
    ev.preventDefault();
    await JLAuth.logout();
    location.replace(JL_LOGIN_PAGE);
  });
}

/** Fetch the admin-managed logo and apply it to the admin shell (sidebar "JL"
    box + favicon). Best-effort; the default branding stays if none is set. */
async function jlApplyBranding() {
  try {
    const res = await fetch(JL_API_BASE + "/api/v1/branding");
    if (!res.ok) return;
    const json = await res.json();
    const url = json && json.data && json.data.logoUrl;
    if (!url) return;
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
  } catch (_) { /* branding is optional */ }
}

document.addEventListener("DOMContentLoaded", jlApplyBranding);
