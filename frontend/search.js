/* ════════════════════════════════════════════════════════════════════
   JL ENTERPRISES — storefront UI enhancements (progressive enhancement).

   TWO things run here, both on every storefront page:
     A) injectPolish() — site-wide interaction polish: consistent hover states,
        a visible keyboard focus ring (a11y), smooth transitions, press feedback.
     B) the enhanced search bar (below) — a no-op on pages without a search form.


   Upgrades the plain header search form into a modern search experience:
     • prominent, branded styling with a search icon + clear (×) button
     • API-powered instant suggestions (debounced) with product previews
     • recent searches (localStorage) + popular searches when empty
     • loading indicator, empty-state message, full keyboard navigation
   It self-initialises and NO-OPS on pages without a header search form, so
   this one file can be included everywhere. If JS fails, the underlying
   <form action="index.html" method="get" name="q"> still works.

   Depends on globals from store.js (loaded first): JLStore, jlInr, jlEsc,
   JL_CAT_EMOJI. Falls back gracefully if any are missing.
   ════════════════════════════════════════════════════════════════════ */
(function () {
  "use strict";

  var RECENT_KEY = "jl_recent_searches";
  var MAX_RECENT = 6;
  var DEBOUNCE_MS = 220;
  var POPULAR = ["Air Conditioner", "Refrigerator", "Television", "Washing Machine",
    "Kitchen", "Furniture", "LG", "Samsung"];

  var ICON_SEARCH = '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>';
  var ICON_CLEAR = '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><line x1="6" y1="6" x2="18" y2="18"></line><line x1="18" y1="6" x2="6" y2="18"></line></svg>';
  var ICON_CLOCK = '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"></circle><polyline points="12 7 12 12 15 14"></polyline></svg>';

  function esc(s) {
    return (typeof jlEsc === "function") ? jlEsc(s)
      : String(s == null ? "" : s).replace(/[&<>"']/g, function (c) {
        return ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c];
      });
  }
  function money(n) { return (typeof jlInr === "function") ? jlInr(n) : "₹" + Math.round(Number(n || 0)); }
  function emojiFor(slug) {
    // JL_CAT_EMOJI is a top-level const in store.js (a lexical global, not on window).
    try { return (typeof JL_CAT_EMOJI !== "undefined" && JL_CAT_EMOJI[slug]) || "📦"; } catch (_) { return "📦"; }
  }
  function onIndex() {
    var p = location.pathname;
    return /(^|\/)index\.html$/.test(p) || /\/$/.test(p);
  }

  // ── recent searches (localStorage) ──
  function getRecent() {
    try { return (JSON.parse(localStorage.getItem(RECENT_KEY)) || []).filter(Boolean); } catch (_) { return []; }
  }
  function pushRecent(term) {
    term = (term || "").trim();
    if (!term) return;
    var list = getRecent().filter(function (t) { return t.toLowerCase() !== term.toLowerCase(); });
    list.unshift(term);
    try { localStorage.setItem(RECENT_KEY, JSON.stringify(list.slice(0, MAX_RECENT))); } catch (_) {}
  }
  function clearRecent() { try { localStorage.removeItem(RECENT_KEY); } catch (_) {} }

  // ── one-time styles ──
  function injectStyles() {
    if (document.getElementById("jl-search-styles")) return;
    var css = ''
      + '.jl-search{position:relative;flex:1;display:flex;align-items:center;gap:6px;min-width:0;'
      + 'background:#fff;border:2.5px solid #576cbc;border-radius:14px;padding:0 6px 0 12px;'
      + 'transition:border-color .18s ease,box-shadow .18s ease;box-shadow:0 2px 6px rgba(15,23,42,.10)}'
      + '.jl-search:hover{border-color:#0b2447}'
      + '.jl-search.is-focus{border-color:#0b2447;box-shadow:0 0 0 3px rgba(87,108,188,.25),0 10px 24px rgba(15,23,42,.12)}'
      + '.jl-search-ic{display:flex;color:#94a3b8;flex:0 0 auto;transition:color .18s ease}'
      + '.jl-search.is-focus .jl-search-ic{color:#576cbc}'
      + '.jl-search-input{flex:1;min-width:0;border:0;outline:0;background:transparent;padding:11px 4px;'
      + 'font-size:15px;color:#0b2447;font-family:inherit}'
      + '.jl-search-input::placeholder{color:#94a3b8}'
      + '.jl-search-clear{flex:0 0 auto;display:none;align-items:center;justify-content:center;width:26px;height:26px;'
      + 'border:0;border-radius:50%;background:#f1f5f9;color:#64748b;cursor:pointer;transition:background .15s,color .15s}'
      + '.jl-search-clear:hover{background:#e2e8f0;color:#0b2447}'
      + '.jl-search.has-text .jl-search-clear{display:flex}'
      + '.jl-search-spin{flex:0 0 auto;width:18px;height:18px;border:2px solid #e2e8f0;border-top-color:#576cbc;'
      + 'border-radius:50%;animation:jlspin .7s linear infinite;display:none}'
      + '.jl-search.is-loading .jl-search-spin{display:block}'
      + '.jl-search.is-loading .jl-search-clear{display:none}'
      + '@keyframes jlspin{to{transform:rotate(360deg)}}'
      + '.jl-search-go{flex:0 0 auto;display:flex;align-items:center;gap:7px;border:0;cursor:pointer;'
      + 'background:#0b2447;color:#fff;font-weight:700;font-size:14px;border-radius:10px;padding:9px 16px;margin:4px 2px;'
      + 'transition:background .16s ease}'
      + '.jl-search-go:hover{background:#f97316}'
      + '.jl-search-panel{position:absolute;top:calc(100% + 10px);left:0;right:0;z-index:60;background:#fff;'
      + 'border:1px solid #e8edf3;border-radius:16px;box-shadow:0 18px 44px rgba(15,23,42,.16);'
      + 'padding:8px;max-height:min(74vh,520px);overflow:auto;overscroll-behavior:contain}'
      + '.jl-search-panel[hidden]{display:none}'
      + '.jl-sec{display:flex;align-items:center;justify-content:space-between;padding:9px 12px 5px;'
      + 'font-size:11px;letter-spacing:.09em;text-transform:uppercase;color:#94a3b8;font-weight:700}'
      + '.jl-sec button{border:0;background:none;color:#576cbc;font-size:11px;font-weight:700;cursor:pointer;letter-spacing:0;text-transform:none}'
      + '.jl-chips{display:flex;flex-wrap:wrap;gap:8px;padding:2px 10px 10px}'
      + '.jl-chip{display:inline-flex;align-items:center;gap:6px;border:1px solid #e2e8f0;background:#f8fafc;'
      + 'color:#334155;border-radius:999px;padding:7px 13px;font-size:13px;cursor:pointer;transition:all .14s ease}'
      + '.jl-chip:hover,.jl-chip.active{background:#576cbc;border-color:#576cbc;color:#fff}'
      + '.jl-chip svg{opacity:.6}'
      + '.jl-sug{display:flex;align-items:center;gap:12px;padding:9px 12px;border-radius:11px;cursor:pointer;text-decoration:none}'
      + '.jl-sug:hover,.jl-sug.active{background:#f1f5f9}'
      + '.jl-sug-thumb{flex:0 0 auto;width:44px;height:44px;border-radius:10px;overflow:hidden;display:flex;align-items:center;'
      + 'justify-content:center;font-size:22px;background:linear-gradient(135deg,#f1f5f9,#e2e8f0)}'
      + '.jl-sug-thumb img{width:100%;height:100%;object-fit:cover}'
      + '.jl-sug-body{flex:1;min-width:0}'
      + '.jl-sug-name{font-size:13.5px;color:#0b2447;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}'
      + '.jl-sug-name mark{background:#fef3c7;color:inherit;padding:0 1px;border-radius:2px}'
      + '.jl-sug-meta{font-size:12px;color:#94a3b8;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}'
      + '.jl-sug-price{flex:0 0 auto;font-size:13.5px;font-weight:800;color:#0b2447}'
      + '.jl-sug-all{display:flex;align-items:center;gap:10px;padding:11px 12px;border-radius:11px;cursor:pointer;color:#576cbc;font-weight:700;font-size:13.5px}'
      + '.jl-sug-all:hover,.jl-sug-all.active{background:#eef2fb}'
      + '.jl-sug-all .jl-search-ic{color:#576cbc}'
      + '.jl-empty{padding:22px 14px;text-align:center;color:#94a3b8;font-size:13.5px}'
      + '.jl-sep{height:1px;background:#f1f5f9;margin:6px 8px}'
      + '@media(max-width:640px){.jl-search-go span{display:none}.jl-search-go{padding:9px 12px}.jl-search-input{font-size:16px}}';
    var st = document.createElement("style");
    st.id = "jl-search-styles";
    st.textContent = css;
    document.head.appendChild(st);
  }

  function highlight(name, term) {
    var safe = esc(name);
    var t = (term || "").trim();
    if (!t) return safe;
    try {
      var re = new RegExp("(" + t.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") + ")", "ig");
      return safe.replace(re, "<mark>$1</mark>");
    } catch (_) { return safe; }
  }

  function enhance(form) {
    if (!form || form.classList.contains("jl-search")) return;
    var oldInput = form.querySelector('input[name="q"], #searchInput');
    var initialValue = oldInput ? oldInput.value : "";

    // Replace the form's utility classes entirely — the originals include
    // `overflow-hidden` (would clip the dropdown) and rounded-full/bg-slate-100
    // that fight our component styles. `.jl-search` provides flex/border/radius.
    form.className = "jl-search";
    form.setAttribute("role", "search");
    form.innerHTML =
      '<span class="jl-search-ic" aria-hidden="true">' + ICON_SEARCH + '</span>' +
      '<input id="searchInput" name="q" type="text" autocomplete="off" spellcheck="false" ' +
      'class="jl-search-input" placeholder="Search for appliances, brands, or products…" ' +
      'role="combobox" aria-expanded="false" aria-autocomplete="list" aria-controls="jlSearchPanel" aria-label="Search products" />' +
      '<span class="jl-search-spin" aria-hidden="true"></span>' +
      '<button type="button" class="jl-search-clear" aria-label="Clear search" tabindex="-1">' + ICON_CLEAR + '</button>' +
      '<button type="submit" class="jl-search-go">' + ICON_SEARCH + '<span>Search</span></button>' +
      '<div class="jl-search-panel" id="jlSearchPanel" role="listbox" aria-label="Search suggestions" hidden></div>';

    var input = form.querySelector(".jl-search-input");
    var clearBtn = form.querySelector(".jl-search-clear");
    var panel = form.querySelector(".jl-search-panel");
    if (initialValue) { input.value = initialValue; form.classList.add("has-text"); }

    var options = [];      // [{el, activate()}]
    var activeIdx = -1;
    var seq = 0;           // guards against out-of-order async responses
    var debounceTimer = null;

    function setActive(i) {
      if (!options.length) { activeIdx = -1; input.removeAttribute("aria-activedescendant"); return; }
      if (i < 0) i = options.length - 1;
      if (i >= options.length) i = 0;
      options.forEach(function (o, idx) { o.el.classList.toggle("active", idx === i); });
      activeIdx = i;
      var el = options[i].el;
      if (el.id) input.setAttribute("aria-activedescendant", el.id);
      if (el.scrollIntoView) el.scrollIntoView({ block: "nearest" });
    }
    function openPanel() { panel.hidden = false; input.setAttribute("aria-expanded", "true"); }
    function closePanel() {
      panel.hidden = true; input.setAttribute("aria-expanded", "false");
      input.removeAttribute("aria-activedescendant"); activeIdx = -1;
    }
    function collectOptions() {
      options = [].slice.call(panel.querySelectorAll("[data-opt]")).map(function (el, i) {
        el.id = "jlsug-" + i;
        el.setAttribute("role", "option");
        return { el: el, activate: function () { el.__activate && el.__activate(); } };
      });
      activeIdx = -1;
    }

    function goProduct(slug, name) { pushRecent(name || ""); location.href = "product.html?slug=" + encodeURIComponent(slug); }
    function runSearch(term) {
      term = (term || "").trim();
      if (!term) { input.focus(); return; }
      pushRecent(term);
      if (onIndex() && (typeof window.jlHomeSearch === "function" || typeof applySearch === "function")) {
        input.value = term; closePanel();
        // Prefer the API-backed grid reload (finds matches on any page); fall back to on-page filter.
        if (typeof window.jlHomeSearch === "function") window.jlHomeSearch(term);
        else applySearch(term);
        var grid = document.getElementById("products");
        if (grid) grid.scrollIntoView({ behavior: "smooth" });
      } else {
        location.href = "index.html?q=" + encodeURIComponent(term);
      }
    }

    // ── panel content builders ──
    function renderDefault() {
      var recent = getRecent();
      var html = "";
      if (recent.length) {
        html += '<div class="jl-sec">Recent<button type="button" data-clear-recent>Clear</button></div><div class="jl-chips">';
        html += recent.map(function (t) {
          return '<span class="jl-chip" data-opt data-term="' + esc(t) + '">' + ICON_CLOCK + esc(t) + '</span>';
        }).join("");
        html += '</div>';
      }
      html += '<div class="jl-sec">Popular searches</div><div class="jl-chips">';
      html += POPULAR.map(function (t) {
        return '<span class="jl-chip" data-opt data-term="' + esc(t) + '">' + esc(t) + '</span>';
      }).join("");
      html += '</div>';
      panel.innerHTML = html;
      var cr = panel.querySelector("[data-clear-recent]");
      if (cr) cr.addEventListener("mousedown", function (e) { e.preventDefault(); clearRecent(); renderDefault(); collectOptions(); });
      panel.querySelectorAll("[data-term]").forEach(function (el) {
        el.__activate = function () { runSearch(el.getAttribute("data-term")); };
        el.addEventListener("mousedown", function (e) { e.preventDefault(); el.__activate(); });
      });
      collectOptions();
      openPanel();
    }

    function allRow(term) {
      return '<div class="jl-sug-all" data-opt data-all="' + esc(term) + '">' + ICON_SEARCH +
        'Search for “' + esc(term) + '”</div>';
    }

    function renderResults(term, items) {
      var html = allRow(term);
      if (items.length) {
        html += '<div class="jl-sep"></div><div class="jl-sec">Products</div>';
        html += items.map(function (p) {
          var thumb = p.primaryImageUrl
            ? '<img src="' + esc(p.primaryImageUrl) + '" alt="" />'
            : emojiFor(p.categorySlug);
          var meta = p.brandName || "";
          return '<a class="jl-sug" data-opt data-slug="' + esc(p.slug) + '" data-name="' + esc(p.name) + '" ' +
            'href="product.html?slug=' + encodeURIComponent(p.slug) + '">' +
            '<span class="jl-sug-thumb">' + thumb + '</span>' +
            '<span class="jl-sug-body"><span class="jl-sug-name">' + highlight(p.name, term) + '</span>' +
            (meta ? '<span class="jl-sug-meta">' + esc(meta) + '</span>' : '') + '</span>' +
            '<span class="jl-sug-price">' + money(p.price) + '</span></a>';
        }).join("");
      } else {
        html += '<div class="jl-empty">No products match “' + esc(term) + '”.<br>Press Enter to search the full store.</div>';
      }
      panel.innerHTML = html;
      panel.querySelectorAll("[data-slug]").forEach(function (el) {
        el.__activate = function () { goProduct(el.getAttribute("data-slug"), el.getAttribute("data-name")); };
        el.addEventListener("click", function (e) { e.preventDefault(); el.__activate(); });
      });
      var all = panel.querySelector("[data-all]");
      if (all) {
        all.__activate = function () { runSearch(all.getAttribute("data-all")); };
        all.addEventListener("mousedown", function (e) { e.preventDefault(); all.__activate(); });
      }
      collectOptions();
      openPanel();
    }

    function fetchSuggestions(term) {
      var my = ++seq;
      form.classList.add("is-loading");
      var req = (typeof JLStore !== "undefined" && JLStore.products)
        ? JLStore.products({ search: term, size: 7 })
        : Promise.reject(new Error("no api"));
      req.then(function (page) {
        if (my !== seq) return;                       // a newer keystroke superseded this
        form.classList.remove("is-loading");
        renderResults(term, (page && page.content) || []);
      }).catch(function () {
        if (my !== seq) return;
        form.classList.remove("is-loading");
        renderResults(term, []);
      });
    }

    function onInput() {
      var v = input.value;
      form.classList.toggle("has-text", !!v);
      if (typeof applySearch === "function" && onIndex()) applySearch(v);   // keep live grid filter on home
      clearTimeout(debounceTimer);
      var term = v.trim();
      if (!term) { seq++; form.classList.remove("is-loading"); renderDefault(); return; }
      debounceTimer = setTimeout(function () { fetchSuggestions(term); }, DEBOUNCE_MS);
    }

    // ── events ──
    input.addEventListener("input", onInput);
    input.addEventListener("focus", function () {
      form.classList.add("is-focus");
      if (input.value.trim()) { if (panel.hidden) fetchSuggestions(input.value.trim()); }
      else renderDefault();
    });
    form.addEventListener("focusout", function () {
      // close only when focus leaves the whole component
      setTimeout(function () {
        if (!form.contains(document.activeElement)) { form.classList.remove("is-focus"); closePanel(); }
      }, 0);
    });
    clearBtn.addEventListener("click", function () {
      input.value = ""; form.classList.remove("has-text"); seq++;
      if (onIndex()) {
        if (typeof window.jlHomeSearch === "function") window.jlHomeSearch("");   // restore full catalog
        else if (typeof applySearch === "function") applySearch("");
      }
      renderDefault(); input.focus();
    });
    form.addEventListener("submit", function (e) {
      e.preventDefault();
      if (activeIdx >= 0 && options[activeIdx]) { options[activeIdx].activate(); return; }
      runSearch(input.value);
    });
    input.addEventListener("keydown", function (e) {
      if (e.key === "ArrowDown") { e.preventDefault(); if (panel.hidden) onInput(); setActive(activeIdx + 1); }
      else if (e.key === "ArrowUp") { e.preventDefault(); setActive(activeIdx - 1); }
      else if (e.key === "Escape") { if (!panel.hidden) { e.preventDefault(); closePanel(); } }
      else if (e.key === "Enter") {
        if (activeIdx >= 0 && options[activeIdx]) { e.preventDefault(); options[activeIdx].activate(); }
      }
    });

    // If the page opened with ?q=, reflect it (home also filters the grid via its own loader).
    try {
      var q = new URLSearchParams(location.search).get("q");
      if (q && !input.value) { input.value = q; form.classList.add("has-text"); }
    } catch (_) {}
  }

  // ── Global interaction polish (applies to every storefront page) ──────
  // Adds consistent hover feedback to elements that lacked it (header nav
  // links, footer links), a visible keyboard focus ring on all interactive
  // elements (accessibility), smooth transitions, and press feedback. Injected
  // last so it wins CSS ties; written to NOT fight existing Tailwind hovers
  // (the navy category strip keeps its own hover via higher specificity, and
  // the search input keeps its own focus ring via :not()).
  function injectPolish() {
    if (document.getElementById("jl-ui-polish")) return;
    var css = ''
      + 'a,button,[role="button"],input,select,textarea,.product-card,summary{'
      + 'transition:color .18s ease,background-color .18s ease,border-color .18s ease,'
      + 'box-shadow .18s ease,transform .16s ease,opacity .18s ease}'
      // keyboard focus visibility — the search input keeps its own ring
      + 'a:focus-visible,button:focus-visible,[role="button"]:focus-visible,[tabindex]:focus-visible,'
      + 'select:focus-visible,textarea:focus-visible,input:focus-visible:not(.jl-search-input){'
      + 'outline:2px solid #576cbc;outline-offset:2px}'
      // header top-nav links (Service, Sign Up, Login, Cart, My Orders, Logout, Profile)
      + 'header>div>nav a:hover{color:#576cbc}'
      + 'header>div>nav a>span:first-child{display:inline-block;transition:transform .16s ease}'
      + 'header>div>nav a:hover>span:first-child{transform:translateY(-2px) scale(1.08)}'
      // press feedback on nav links and any button
      + 'header>div>nav a:active,button:not(:disabled):active{transform:scale(.96)}'
      // footer links
      + 'footer a{transition:color .16s ease}'
      + 'footer a:hover{color:#fff}'
      // ensure pointer cursor on card actions
      + '.product-card a,.product-card button{cursor:pointer}';
    var st = document.createElement("style");
    st.id = "jl-ui-polish";
    st.textContent = css;
    (document.head || document.documentElement).appendChild(st);
  }

  function boot() {
    injectPolish();                           // site-wide interaction polish (runs on every page)
    var input = document.querySelector('header input[name="q"], header #searchInput, #searchInput');
    if (!input) return;                       // no header search on this page → search enhancement is a no-op
    var form = input.closest("form");
    if (!form) return;
    injectStyles();
    enhance(form);
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", boot);
  else boot();
})();
