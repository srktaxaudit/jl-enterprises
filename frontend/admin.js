/* ══════════════════════════════════════════════════════════════════════
   JL Enterprises — Admin frontend, wired to the ecommerce-backend API.
   Bearer-token auth against /api/v1/auth. Responses use the standard envelope
   { success, message, data, timestamp } — jlApi() returns the `data` payload.

   NOTE: tokens live in localStorage for simplicity. That is convenient but
   XSS-exposed; for higher security move to httpOnly cookies + a token endpoint.
   ══════════════════════════════════════════════════════════════════════ */

// Where the Spring Boot API lives. Localhost during dev; set the prod URL below.
const JL_API_BASE = (() => {
  const h = location.hostname;
  if (h === "localhost" || h === "127.0.0.1" || h === "") return "http://localhost:8081";
  // TODO: replace with your deployed API origin (e.g. https://jl-ecommerce-api.onrender.com)
  return "https://jl-ecommerce-api.onrender.com";
})();

const JL_LOGIN_PAGE = "admin-login.html";
const ACCESS_KEY = "jl_access";
const REFRESH_KEY = "jl_refresh";
const STAFF_ROLES = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_MANAGER"];

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

  const res = await fetch(JL_API_BASE + path, opts);

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

/**
 * Guard a protected admin page. Redirects to login if unauthenticated, or back
 * to the store if the user isn't staff. Resolves with the current UserDto.
 */
async function jlRequireAdmin() {
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
