(function () {
  "use strict";

  const NAV = [
    ["Overview",[["Dashboard","📊","admin.html"],["Notifications","🔔","admin-notifications.html"],["Orders","📥","admin-orders.html","ORDER_MANAGER,CUSTOMER_SUPPORT"]]],
    ["Catalog & Sales",[["Products","📦","admin-products.html","PRODUCT_MANAGER"],["Inventory","🗂️","admin-inventory.html","INVENTORY_MANAGER"],["Offers & Deals","🏷️","admin-offers.html","MARKETING_MANAGER"]]],
    ["Engage",[["Customers (CRM)","👥","admin-customers.html","MANAGER"],["Reviews","⭐","admin-reviews.html","MARKETING_MANAGER,CUSTOMER_SUPPORT"],["Service Bookings","🔧","admin-service.html","CUSTOMER_SUPPORT"],["Exchange Requests","♻️","admin-exchanges.html","CUSTOMER_SUPPORT,MANAGER"],["WhatsApp Marketing","💬","admin-whatsapp.html","MARKETING_MANAGER"]]],
    ["Human Resources",[["Employees & Payroll","🧑‍🤝‍🧑","admin-hr.html","@hr"]]],
    ["Accounting",[["Billing","🧾","admin-billing.html","@admin"],["Chart of Accounts","📒","admin-accounts.html","ACCOUNTANT"],["Invoices & Bills","🧾","admin-vouchers.html","ACCOUNTANT"],["Journal / Vouchers","✍️","admin-journal.html","ACCOUNTANT"],["Ledgers","📚","admin-ledgers.html","ACCOUNTANT"],["Financial Reports","📈","admin-reports.html","ACCOUNTANT"],["GST Returns","🧮","admin-gst.html","ACCOUNTANT"],["Outstanding & Cashflow","📆","admin-outstanding.html","ACCOUNTANT"]]],
    ["Control",[["Staff","🧑‍💼","admin-staff.html","@admin"],["Team & Roles","👤","admin-team.html","@admin"],["Activity Logs","📜","admin-logs.html","@admin"],["Import / Export","🔄","admin-data.html","ACCOUNTANT"],["Logo & Branding","🖼️","admin-branding.html","@admin"],["Settings","⚙️","admin-settings.html","@admin"]]]
  ];
  // Which sidebar links get a "needs attention" badge, and how to derive the number
  // from the /section-counts payload. (Enquiries/EMI wired in Phase 4.)
  const BADGE_MAP = {
    "admin-orders.html": c => (c.ordersPending || 0) + (c.returnRequests || 0),
    "admin-service.html": c => c.serviceBookingsNew || 0,
    "admin-exchanges.html": c => c.exchangesPending || 0,
    "admin-reviews.html": c => c.reviewsPending || 0,
    "admin-inventory.html": c => c.lowStock || 0,
    "admin-notifications.html": c => c.unreadNotifications || 0,
    "admin-enquiries.html": c => c.contactEnquiriesNew || 0,
    "admin-emi-requests.html": c => c.emiRequestsNew || 0
  };
  const shell = document.getElementById("adminShell");
  const nav = document.getElementById("adminNav");
  const frame = document.getElementById("adminFrame");
  const title = document.getElementById("pageTitle");
  const allowedPages = new Set(NAV.flatMap(group => group[1].map(item => item[2])));
  const labels = Object.fromEntries(NAV.flatMap(group => group[1].map(item => [item[2], item[0]])));
  // The "Accounting" group renders as a collapsible parent menu. Its expanded
  // state persists in localStorage; the pages below tell us when it holds the
  // active page (so we can highlight the parent + default-open on that page).
  const ACCT_KEY = "jl_admin_nav_acct";
  const ACCT_GROUP = NAV.find(g => g[0] === "Accounting");
  const ACCOUNTING_PAGES = new Set((ACCT_GROUP ? ACCT_GROUP[1] : []).map(it => it[2]));
  let currentPage = "admin.html";

  function cleanPage(value) {
    try {
      const url = new URL(value || "admin.html", location.href);
      const file = url.pathname.split("/").pop();
      return allowedPages.has(file) ? file + url.search + url.hash : "admin.html";
    } catch (_) { return "admin.html"; }
  }
  function fileOf(value) { return value.split(/[?#]/)[0]; }
  function visible(user, rule) {
    if (!rule) return true;
    if (rule === "@admin") return jlIsSuper(user);
    if (rule === "@hr") return jlIsSuper(user) || jlHasRole(user, "HR");
    return jlIsSuper(user) || jlHasRole(user, "MANAGER", ...rule.split(","));
  }
  function renderNav(user) {
    nav.innerHTML = NAV.map(group => {
      const items = group[1].filter(item => visible(user, item[3]));
      if (!items.length) return "";
      const links = items.map(item => {
        const badge = BADGE_MAP[item[2]] ? `<span class="nav-badge" data-badge-href="${item[2]}" hidden></span>` : "";
        return `<a href="${item[2]}" data-page="${item[2]}" title="${item[0]}"><span class="icon">${item[1]}</span><span class="label">${item[0]}</span>${badge}</a>`;
      }).join("");
      // Only the Accounting group is a collapsible parent; every other group
      // stays a plain section header (unaffected).
      if (group[0] === "Accounting") {
        const saved = localStorage.getItem(ACCT_KEY);
        const open = saved === null ? ACCOUNTING_PAGES.has(fileOf(currentPage)) : saved === "1";
        return `<div class="admin-nav-group-wrap${open ? " open" : ""}" data-acct>
          <button type="button" class="admin-nav-parent" aria-expanded="${open}" aria-controls="jlAcctSub">
            <span class="icon">🧮</span><span class="label">Accounting</span><span class="chev" aria-hidden="true">▾</span>
          </button>
          <div class="admin-nav-sub" id="jlAcctSub" role="group" aria-label="Accounting"><div>${links}</div></div>
        </div>`;
      }
      return `<div class="admin-nav-group">${group[0]}</div>` + links;
    }).join("");
    setActive(currentPage);
  }
  function setActive(page) {
    const file = fileOf(page);
    nav.querySelectorAll("a[data-page]").forEach(a => {
      const active = a.dataset.page === file;
      a.classList.toggle("active", active);
      if (active) a.setAttribute("aria-current", "page"); else a.removeAttribute("aria-current");
    });
    // Mark the collapsible Accounting parent when it holds the active page.
    const acctWrap = nav.querySelector(".admin-nav-group-wrap[data-acct]");
    if (acctWrap) acctWrap.classList.toggle("has-active", ACCOUNTING_PAGES.has(file));
    title.textContent = labels[file] || "Admin";
    document.title = `${labels[file] || "Admin"} — JL Admin`;
  }
  function rememberNavScroll() { sessionStorage.setItem("jl_admin_nav_scroll", String(nav.scrollTop)); }
  function navigate(page, push) {
    page = cleanPage(page);
    if (page === currentPage && frame.src) { closeMobile(); return; }
    rememberNavScroll();
    currentPage = page;
    setActive(page);
    frame.src = page;
    if (push) history.pushState({ page }, "", "admin-shell.html?page=" + encodeURIComponent(page));
    closeMobile();
  }
  function closeMobile() { shell.classList.remove("mobile-open"); document.getElementById("menuBtn").setAttribute("aria-expanded", "false"); }
  function prepareFrame() {
    try {
      const doc = frame.contentDocument;
      const file = frame.contentWindow.location.pathname.split("/").pop();
      if (allowedPages.has(file)) {
        currentPage = file + frame.contentWindow.location.search + frame.contentWindow.location.hash;
        setActive(currentPage);
      }
      doc.body.classList.add("jl-admin-embedded");
      let style = doc.getElementById("jl-admin-embedded-style");
      if (!style) {
        style = doc.createElement("style"); style.id = "jl-admin-embedded-style";
        style.textContent = `html,body{min-height:100%!important}body{background:#eef1f6!important}.jl-admin-embedded>[class*="sticky"][class*="top-0"]{display:none!important}.jl-admin-embedded>div.md\\:flex>aside{display:none!important}.jl-admin-embedded>div.md\\:flex{display:block!important}.jl-admin-embedded>div.md\\:flex>div{width:100%!important}.jl-admin-embedded>div.md\\:flex>div>[class*="sticky"][class*="top-0"]{display:none!important}@media(max-width:767px){.jl-admin-embedded [class*="max-w-"]{padding-left:14px!important;padding-right:14px!important}}`;
        doc.head.appendChild(style);
      }
      // Keep links inside the workspace and let the shell own visible navigation state.
      doc.addEventListener("click", function (event) {
        const a = event.target.closest("a[href]");
        if (!a) return;
        const href = a.getAttribute("href");
        if (href && allowedPages.has(fileOf(href))) {
          event.preventDefault(); navigate(href, true);
        }
      });
    } catch (_) { /* same-origin pages are expected; fail open if hosting changes */ }
  }

  if (localStorage.getItem("jl_admin_sidebar_collapsed") === "1") shell.classList.add("collapsed");
  document.getElementById("collapseBtn").addEventListener("click", () => {
    shell.classList.toggle("collapsed");
    localStorage.setItem("jl_admin_sidebar_collapsed", shell.classList.contains("collapsed") ? "1" : "0");
  });
  document.getElementById("menuBtn").addEventListener("click", () => {
    const open = shell.classList.toggle("mobile-open");
    document.getElementById("menuBtn").setAttribute("aria-expanded", String(open));
  });
  document.getElementById("adminScrim").addEventListener("click", closeMobile);
  document.addEventListener("keydown", e => { if (e.key === "Escape") closeMobile(); });
  nav.addEventListener("scroll", rememberNavScroll, { passive:true });
  nav.addEventListener("click", e => {
    const a = e.target.closest("a[data-page]");
    if (!a) return;
    e.preventDefault(); navigate(a.dataset.page, true);
  });
  // Toggle the collapsible Accounting parent (mouse + keyboard via the button).
  nav.addEventListener("click", e => {
    const parent = e.target.closest(".admin-nav-parent");
    if (!parent) return;
    const wrap = parent.closest(".admin-nav-group-wrap");
    const open = !wrap.classList.contains("open");
    wrap.classList.toggle("open", open);
    parent.setAttribute("aria-expanded", String(open));
    try { localStorage.setItem(ACCT_KEY, open ? "1" : "0"); } catch (_) { /* private mode */ }
  });
  frame.addEventListener("load", prepareFrame);
  addEventListener("popstate", e => navigate((e.state && e.state.page) || new URLSearchParams(location.search).get("page"), false));
  document.getElementById("shellLogout").addEventListener("click", async () => { await JLAuth.logout(); location.replace(JL_LOGIN_PAGE); });

  // ── Notification bell (header) — opens the notifications page + shows unread count ──
  const bellBtn = document.getElementById("bellBtn");
  const bellBadge = document.getElementById("bellBadge");
  if (bellBtn) bellBtn.addEventListener("click", () => navigate("admin-notifications.html", true));
  async function refreshBell() {
    try {
      const r = await jlApi("/api/v1/notifications/unread-count", { blocking: false });
      const n = (r && r.unread) || 0;
      if (n > 0) { bellBadge.textContent = n > 99 ? "99+" : String(n); bellBadge.hidden = false; }
      else bellBadge.hidden = true;
    } catch (_) { /* best-effort */ }
  }
  window.jlRefreshBell = refreshBell;   // let the notifications page refresh the badge after mark-read
  setInterval(refreshBell, 45000);
  addEventListener("focus", refreshBell);

  // ── Sidebar "needs attention" badges ──
  function applyBadges(counts) {
    nav.querySelectorAll("[data-badge-href]").forEach(el => {
      const fn = BADGE_MAP[el.getAttribute("data-badge-href")];
      const n = fn ? fn(counts) : 0;
      if (n > 0) { el.textContent = n > 99 ? "99+" : String(n); el.hidden = false; }
      else el.hidden = true;
    });
  }
  async function refreshCounts() {
    try { applyBadges(await jlApi("/api/v1/admin/section-counts", { blocking: false }) || {}); }
    catch (_) { /* best-effort — badges just stay as they are */ }
  }
  window.jlRefreshCounts = refreshCounts;   // pages can nudge a refresh after they change a status
  setInterval(refreshCounts, 45000);
  addEventListener("focus", refreshCounts);
  // Refresh after navigating between admin pages (e.g. right after actioning an item).
  frame.addEventListener("load", () => { refreshCounts(); refreshBell(); });

  currentPage = cleanPage(new URLSearchParams(location.search).get("page"));
  jlRequireAdmin().then(user => {
    renderNav(user);
    const name = jlDisplayName(user);
    const initials = name.split(/[\s@.]+/).filter(Boolean).slice(0,2).map(w => w[0].toUpperCase()).join("");
    const badge = document.getElementById("shellBadge"); badge.textContent = initials || "JL"; badge.title = name;
    requestAnimationFrame(() => { nav.scrollTop = Number(sessionStorage.getItem("jl_admin_nav_scroll") || 0); });
    navigate(currentPage, false);
    refreshBell();
    refreshCounts();
  });
})();
