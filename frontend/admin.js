/* ══════════════════════════════════════════════════════════════════════
   JL Enterprises — Admin frontend, wired to the ecommerce-backend API.
   Bearer-token auth against /api/v1/auth. Responses use the standard envelope
   { success, message, data, timestamp } — jlApi() returns the `data` payload.

   NOTE: tokens live in localStorage for simplicity. That is convenient but
   XSS-exposed; for higher security move to httpOnly cookies + a token endpoint.
   ══════════════════════════════════════════════════════════════════════ */

// Where the Spring Boot API lives. Localhost during dev; set the prod URL below.
const JL_API_BASE = (() => {
  // The backend runs only on Render, so always call it (even from a local server).
  return "https://jl-enterprises-api.onrender.com";
})();

const JL_LOGIN_PAGE = "admin-login.html";
const ACCESS_KEY = "jl_access";
const REFRESH_KEY = "jl_refresh";
const STAFF_ROLES = [
  "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_MANAGER",
  "ROLE_INVENTORY_MANAGER", "ROLE_ORDER_MANAGER", "ROLE_PRODUCT_MANAGER",
  "ROLE_MARKETING_MANAGER", "ROLE_CUSTOMER_SUPPORT",
];
// Full-access roles that can see/do everything in the admin.
const SUPER_ROLES = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN"];

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
function jlNotifyWaking() { if (typeof window.JL_ON_WAKE === "function") { try { window.JL_ON_WAKE(); } catch (_) { /* noop */ } } }

async function jlFetchWithRetry(url, opts) {
  for (let i = 0; i <= JL_RETRY_DELAYS.length; i++) {
    try {
      const res = await fetch(url, opts);
      if ([502, 503, 504].includes(res.status) && i < JL_RETRY_DELAYS.length) {
        jlNotifyWaking(); await jlSleep(JL_RETRY_DELAYS[i]); continue;
      }
      return res;
    } catch (_) {
      if (i < JL_RETRY_DELAYS.length) { jlNotifyWaking(); await jlSleep(JL_RETRY_DELAYS[i]); continue; }
      throw { status: 0, message: "Can't reach the server. It may be starting up — please wait a moment and try again." };
    }
  }
}

/**
 * fetch() wrapper. Adds the bearer token, unwraps the ApiResponse envelope, and
 * on a 401 tries a single token refresh before giving up. Returns `data`.
 * Throws { status, message } on failure.
 */
async function jlApi(path, { method = "GET", body, auth = true, _retried = false } = {}) {
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
      return jlApi(path, { method, body, auth, _retried: true });
    }
  }

  let json = null;
  try { json = await res.json(); } catch (_) { /* empty body */ }

  if (!res.ok || (json && json.success === false)) {
    throw { status: res.status, message: (json && json.message) || "Request failed" };
  }
  return json ? json.data : null;
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
  const fd = new FormData();
  fd.append("file", file);
  const headers = {};
  if (jlTokens.access) headers["Authorization"] = "Bearer " + jlTokens.access;

  const res = await jlFetchWithRetry(JL_API_BASE + path, { method: "POST", headers, body: fd });

  if (res.status === 401 && !_retried && jlTokens.refresh) {
    if (await jlTryRefresh()) return jlUpload(path, file, true);
  }
  let json = null;
  try { json = await res.json(); } catch (_) { /* empty */ }
  if (!res.ok || (json && json.success === false)) {
    throw { status: res.status, message: (json && json.message) || "Upload failed" };
  }
  return json ? json.data : null;
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
    const back = encodeURIComponent(location.pathname.split("/").pop() || "admin.html");
    location.replace(JL_LOGIN_PAGE + "?next=" + back);
    return new Promise(() => {});
  }
  if (!jlIsStaff(user)) {
    alert("This account does not have staff access.");
    location.replace("index.html");
    return new Promise(() => {});
  }
  // MANAGER has broad operational access; super-admins always pass.
  if (allowedRoles && allowedRoles.length && !jlIsSuper(user) && !jlHasRole(user, "MANAGER", ...allowedRoles)) {
    alert("You don't have access to this section.");
    location.replace("admin.html");
    return new Promise(() => {});
  }
  return user;
}

/** Guard an admin-only page (SUPER_ADMIN / ADMIN only) — e.g. Staff, Settings. */
async function jlRequireSuper() {
  const user = await jlRequireAdmin();
  if (!jlIsSuper(user)) {
    alert("This section is restricted to administrators.");
    location.replace("admin.html");
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
