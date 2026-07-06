(function () {
  "use strict";

  const NAV = [
    ["Overview",[["Dashboard","📊","admin.html"],["Orders","📥","admin-orders.html","ORDER_MANAGER,CUSTOMER_SUPPORT"],["Billing","🧾","admin-billing.html","@admin"]]],
    ["Catalog & Sales",[["Products","📦","admin-products.html","PRODUCT_MANAGER"],["Inventory","🗂️","admin-inventory.html","INVENTORY_MANAGER"],["Offers & Deals","🏷️","admin-offers.html","MARKETING_MANAGER"]]],
    ["Engage",[["Customers (CRM)","👥","admin-customers.html","CUSTOMER_SUPPORT"],["Reviews","⭐","admin-reviews.html","MARKETING_MANAGER,CUSTOMER_SUPPORT"],["Service Bookings","🔧","admin-service.html","CUSTOMER_SUPPORT"],["WhatsApp Offers","💬","admin-whatsapp.html","MARKETING_MANAGER"]]],
    ["Accounting",[["Chart of Accounts","📒","admin-accounts.html","ACCOUNTANT"],["Invoices & Bills","🧾","admin-vouchers.html","ACCOUNTANT"],["Journal / Vouchers","✍️","admin-journal.html","ACCOUNTANT"],["Ledgers","📚","admin-ledgers.html","ACCOUNTANT"],["Financial Reports","📈","admin-reports.html","ACCOUNTANT"],["GST Returns","🧮","admin-gst.html","ACCOUNTANT"],["Outstanding & Cashflow","📆","admin-outstanding.html","ACCOUNTANT"],["Import / Export","🔄","admin-data.html","ACCOUNTANT"]]],
    ["Control",[["Staff","🧑‍💼","admin-staff.html","@admin"],["Team & Roles","👤","admin-team.html","@admin"],["Activity Logs","📜","admin-logs.html","@admin"],["Logo & Branding","🖼️","admin-branding.html","@admin"],["Settings","⚙️","admin-settings.html","@admin"]]]
  ];
  const shell = document.getElementById("adminShell");
  const nav = document.getElementById("adminNav");
  const frame = document.getElementById("adminFrame");
  const title = document.getElementById("pageTitle");
  const allowedPages = new Set(NAV.flatMap(group => group[1].map(item => item[2])));
  const labels = Object.fromEntries(NAV.flatMap(group => group[1].map(item => [item[2], item[0]])));
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
    return jlIsSuper(user) || jlHasRole(user, "MANAGER", ...rule.split(","));
  }
  function renderNav(user) {
    nav.innerHTML = NAV.map(group => {
      const items = group[1].filter(item => visible(user, item[3]));
      if (!items.length) return "";
      return `<div class="admin-nav-group">${group[0]}</div>` + items.map(item =>
        `<a href="${item[2]}" data-page="${item[2]}" title="${item[0]}"><span class="icon">${item[1]}</span><span class="label">${item[0]}</span></a>`
      ).join("");
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
  frame.addEventListener("load", prepareFrame);
  addEventListener("popstate", e => navigate((e.state && e.state.page) || new URLSearchParams(location.search).get("page"), false));
  document.getElementById("shellLogout").addEventListener("click", async () => { await JLAuth.logout(); location.replace(JL_LOGIN_PAGE); });

  currentPage = cleanPage(new URLSearchParams(location.search).get("page"));
  jlRequireAdmin().then(user => {
    renderNav(user);
    const name = jlDisplayName(user);
    const initials = name.split(/[\s@.]+/).filter(Boolean).slice(0,2).map(w => w[0].toUpperCase()).join("");
    const badge = document.getElementById("shellBadge"); badge.textContent = initials || "JL"; badge.title = name;
    requestAnimationFrame(() => { nav.scrollTop = Number(sessionStorage.getItem("jl_admin_nav_scroll") || 0); });
    navigate(currentPage, false);
  });
})();
