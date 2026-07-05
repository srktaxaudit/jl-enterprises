/* ══════════════════════════════════════════════════════════════════════
   JL Enterprises — shared UI toolkit (loaders, modals, toasts, validation).
   Self-contained: injects its own scoped CSS (`.jlui-*`) so it works the same
   on the Tailwind admin pages and the plain-CSS storefront. No dependencies.

   Public API (all on window):
     jlToast(msg, {type,duration,title})           non-blocking snackbar
     jlAlert(msg, {title,type,okText})    -> Promise professional alert
     jlConfirm(msg, {title,type,confirmText,        -> Promise<boolean>
                     cancelText,danger})
     jlPrompt(msg, {title,defaultValue,             -> Promise<string|null>
                    placeholder,required,validate,inputType})
     jlBusy.show(msg) / jlBusy.hide(tok) / jlBusy.text(msg)   blocking overlay
     jlWithBusy(fnOrPromise, msg)          -> Promise (wraps in overlay)
     jlSpinnerHTML({size,label})           -> string (inline loader markup)
     jlSetError(input,msg) / jlClearError(input) / jlClearErrors(form)
     jlV.*  built-in validators; jlValidate(form, rules); jlField(input, rules)
   ══════════════════════════════════════════════════════════════════════ */
(function () {
  "use strict";
  if (window.__jlui) return;          // load once
  window.__jlui = true;

  // ── scoped styles ──────────────────────────────────────────────────────
  var CSS = `
  :root{--jlui-navy:#0b2447;--jlui-navy6:#19376d;--jlui-brand:#576cbc;--jlui-orange:#f97316;
    --jlui-ok:#16a34a;--jlui-err:#dc2626;--jlui-warn:#d97706;--jlui-info:#2563eb;}
  .jlui-sr{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}
  @keyframes jlui-spin{to{transform:rotate(360deg)}}
  @keyframes jlui-fade{from{opacity:0}to{opacity:1}}
  @keyframes jlui-pop{from{opacity:0;transform:translateY(8px) scale(.97)}to{opacity:1;transform:none}}
  @keyframes jlui-toastin{from{opacity:0;transform:translateY(-12px)}to{opacity:1;transform:none}}
  .jlui-ring{display:inline-block;border-radius:50%;border-style:solid;border-color:var(--jlui-brand);
    border-right-color:transparent;animation:jlui-spin .7s linear infinite;vertical-align:middle}
  /* full-screen blocking overlay — above dialogs/modals so in-modal saves stay covered */
  .jlui-overlay{position:fixed;inset:0;z-index:2147483300;display:flex;flex-direction:column;
    align-items:center;justify-content:center;gap:14px;background:rgba(15,23,42,.35);
    backdrop-filter:blur(4px);-webkit-backdrop-filter:blur(4px);animation:jlui-fade .15s ease;
    font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
  .jlui-overlay .jlui-ring{width:52px;height:52px;border-width:5px}
  .jlui-overlay-card{background:#fff;border-radius:16px;padding:22px 30px;display:flex;flex-direction:column;
    align-items:center;gap:14px;box-shadow:0 20px 50px rgba(2,6,23,.35);max-width:80vw}
  .jlui-overlay-msg{color:var(--jlui-navy);font-weight:600;font-size:15px;text-align:center}
  /* modal */
  .jlui-modal-bg{position:fixed;inset:0;z-index:2147483100;display:flex;align-items:center;justify-content:center;
    padding:16px;background:rgba(15,23,42,.45);backdrop-filter:blur(3px);-webkit-backdrop-filter:blur(3px);
    animation:jlui-fade .15s ease;font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
  .jlui-modal{background:#fff;border-radius:18px;max-width:440px;width:100%;padding:24px;
    box-shadow:0 24px 60px rgba(2,6,23,.4);animation:jlui-pop .18s ease}
  .jlui-modal-ic{display:none;font-size:34px;line-height:1;margin-bottom:6px}
  .jlui-modal-title{font-size:18px;font-weight:700;color:var(--jlui-navy);margin:0 0 6px}
  .jlui-modal-msg{font-size:14px;color:#475569;line-height:1.5;white-space:pre-line}
  .jlui-modal-in{width:100%;margin-top:14px;border:1px solid #cbd5e1;border-radius:9px;padding:10px 12px;font-size:14px;outline:none;font-family:inherit}
  .jlui-modal-in:focus{border-color:var(--jlui-brand);box-shadow:0 0 0 3px rgba(87,108,188,.2)}
  .jlui-modal-err{color:var(--jlui-err);font-size:12.5px;margin-top:6px;min-height:1em}
  .jlui-modal-btns{display:flex;gap:10px;justify-content:flex-end;margin-top:20px;flex-wrap:wrap}
  .jlui-btn{border:0;border-radius:10px;padding:10px 18px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;transition:filter .15s,background .15s}
  .jlui-btn:focus-visible{outline:3px solid rgba(87,108,188,.5);outline-offset:2px}
  .jlui-btn-primary{background:var(--jlui-navy);color:#fff}.jlui-btn-primary:hover{background:var(--jlui-navy6)}
  .jlui-btn-danger{background:var(--jlui-err);color:#fff}.jlui-btn-danger:hover{filter:brightness(.93)}
  .jlui-btn-ghost{background:#f1f5f9;color:#475569}.jlui-btn-ghost:hover{background:#e2e8f0}
  /* content dialog (hosts an existing form/panel, or a built element) */
  .jlui-dialog-bg{align-items:flex-start;overflow-y:auto;padding:24px 16px}
  .jlui-dialog-shell{position:relative;width:100%;max-width:720px;margin:auto;animation:jlui-pop .18s ease}
  .jlui-dialog-shell.lg{max-width:920px}
  .jlui-dialog-shell>*{margin:0!important}
  .jlui-dialog-x{position:absolute;top:12px;right:14px;z-index:3;background:#fff;border:1px solid #e2e8f0;border-radius:8px;
    width:30px;height:30px;font-size:20px;line-height:26px;text-align:center;color:#64748b;cursor:pointer;padding:0}
  .jlui-dialog-x:hover{color:var(--jlui-navy);background:#f8fafc}
  /* toasts */
  .jlui-toasts{position:fixed;top:16px;left:50%;transform:translateX(-50%);z-index:2147483200;
    display:flex;flex-direction:column;gap:10px;width:min(92vw,380px);pointer-events:none;
    font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
  .jlui-toast{pointer-events:auto;display:flex;gap:10px;align-items:flex-start;background:#fff;border-radius:12px;
    padding:12px 14px;box-shadow:0 10px 30px rgba(2,6,23,.18);border-left:5px solid var(--jlui-info);animation:jlui-toastin .2s ease}
  .jlui-toast.ok{border-left-color:var(--jlui-ok)} .jlui-toast.error{border-left-color:var(--jlui-err)}
  .jlui-toast.warn{border-left-color:var(--jlui-warn)} .jlui-toast.info{border-left-color:var(--jlui-info)}
  .jlui-toast-ic{font-size:18px;line-height:1.3}
  .jlui-toast-bd{flex:1;min-width:0}
  .jlui-toast-tt{font-weight:700;font-size:13.5px;color:var(--jlui-navy)}
  .jlui-toast-msg{font-size:13px;color:#475569;word-wrap:break-word}
  .jlui-toast-x{background:none;border:0;color:#94a3b8;font-size:18px;cursor:pointer;line-height:1;padding:0 2px}
  .jlui-toast-x:hover{color:#475569}
  /* inline field validation */
  .jlui-invalid{border-color:var(--jlui-err)!important;box-shadow:0 0 0 3px rgba(220,38,38,.15)!important}
  .jlui-fielderr{color:var(--jlui-err);font-size:12.5px;margin-top:4px}
  @media (prefers-reduced-motion: reduce){
    .jlui-ring{animation-duration:1.2s}
    .jlui-overlay,.jlui-modal-bg,.jlui-modal,.jlui-toast{animation:none}
  }`;
  var st = document.createElement("style");
  st.id = "jlui-styles"; st.textContent = CSS;
  (document.head || document.documentElement).appendChild(st);

  var FOCUSABLE = 'a[href],button:not([disabled]),input:not([disabled]):not([type=hidden]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])';
  function trapFocus(container, e) {
    if (e.key !== "Tab") return;
    var f = container.querySelectorAll(FOCUSABLE);
    if (!f.length) { e.preventDefault(); return; }
    var first = f[0], last = f[f.length - 1];
    if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
  }

  // ── blocking full-screen overlay (reference-counted) ────────────────────
  var busyEl = null, busyCount = 0, busyKeyHandler = null, busyPrevFocus = null;
  function buildBusy(msg) {
    var o = document.createElement("div");
    o.className = "jlui-overlay";
    o.setAttribute("role", "alertdialog");
    o.setAttribute("aria-busy", "true");
    o.setAttribute("aria-live", "assertive");
    o.setAttribute("aria-label", "Loading");
    o.tabIndex = -1;
    o.innerHTML = '<div class="jlui-overlay-card"><span class="jlui-ring" aria-hidden="true"></span>' +
      '<div class="jlui-overlay-msg"></div></div>';
    o.querySelector(".jlui-overlay-msg").textContent = msg || "Please wait…";
    return o;
  }
  var jlBusy = {
    show: function (msg) {
      busyCount++;
      if (!busyEl) {
        busyPrevFocus = document.activeElement;
        busyEl = buildBusy(msg);
        document.body.appendChild(busyEl);
        document.documentElement.style.overflow = "hidden";
        busyEl.focus();
        // Swallow keyboard interaction with the background while blocked.
        busyKeyHandler = function (e) {
          if (["Tab", "Enter", " ", "Spacebar", "Escape", "ArrowUp", "ArrowDown",
               "ArrowLeft", "ArrowRight", "Home", "End", "PageUp", "PageDown"].indexOf(e.key) !== -1) {
            e.preventDefault(); e.stopPropagation();
          }
        };
        document.addEventListener("keydown", busyKeyHandler, true);
      } else if (msg) { jlBusy.text(msg); }
      return busyCount;               // token (informational)
    },
    text: function (msg) { if (busyEl) busyEl.querySelector(".jlui-overlay-msg").textContent = msg; },
    hide: function () {
      busyCount = Math.max(0, busyCount - 1);
      if (busyCount === 0 && busyEl) {
        document.removeEventListener("keydown", busyKeyHandler, true);
        busyKeyHandler = null;
        busyEl.remove(); busyEl = null;
        document.documentElement.style.overflow = "";
        if (busyPrevFocus && busyPrevFocus.focus) { try { busyPrevFocus.focus(); } catch (_) {} }
        busyPrevFocus = null;
      }
    },
    hideAll: function () { busyCount = 1; jlBusy.hide(); },
  };
  window.jlBusy = jlBusy;

  window.jlWithBusy = function (fnOrPromise, msg) {
    jlBusy.show(msg);
    var p = typeof fnOrPromise === "function" ? Promise.resolve().then(fnOrPromise) : Promise.resolve(fnOrPromise);
    return p.finally(function () { jlBusy.hide(); });
  };

  window.jlSpinnerHTML = function (opts) {
    opts = opts || {};
    var size = opts.size || 22, label = opts.label || "Loading…";
    return '<span class="jlui-ring" style="width:' + size + 'px;height:' + size + 'px;border-width:' +
      Math.max(2, Math.round(size / 8)) + 'px" role="status" aria-label="' +
      String(label).replace(/"/g, "&quot;") + '"></span>';
  };

  // ── toasts ──────────────────────────────────────────────────────────────
  var TOAST_IC = { ok: "✓", success: "✓", error: "⚠", warn: "⚠", info: "ℹ" };
  function toastHost() {
    var h = document.querySelector(".jlui-toasts");
    if (!h) {
      h = document.createElement("div");
      h.className = "jlui-toasts";
      h.setAttribute("aria-live", "polite");
      h.setAttribute("aria-atomic", "false");
      document.body.appendChild(h);
    }
    return h;
  }
  window.jlToast = function (message, opts) {
    opts = opts || {};
    var type = ({ success: "ok", ok: "ok", error: "error", warn: "warn", warning: "warn", info: "info" })[opts.type] || "info";
    var el = document.createElement("div");
    el.className = "jlui-toast " + type;
    el.setAttribute("role", type === "error" ? "alert" : "status");
    var ic = document.createElement("span"); ic.className = "jlui-toast-ic"; ic.textContent = TOAST_IC[type] || "ℹ"; ic.setAttribute("aria-hidden", "true");
    var bd = document.createElement("div"); bd.className = "jlui-toast-bd";
    if (opts.title) { var t = document.createElement("div"); t.className = "jlui-toast-tt"; t.textContent = opts.title; bd.appendChild(t); }
    var m = document.createElement("div"); m.className = "jlui-toast-msg"; m.textContent = message; bd.appendChild(m);
    var x = document.createElement("button"); x.className = "jlui-toast-x"; x.type = "button"; x.setAttribute("aria-label", "Dismiss"); x.innerHTML = "&times;";
    var done = false, timer;
    function close() { if (done) return; done = true; clearTimeout(timer); el.style.opacity = "0"; el.style.transition = "opacity .2s"; setTimeout(function () { el.remove(); }, 200); }
    x.addEventListener("click", close);
    el.appendChild(ic); el.appendChild(bd); el.appendChild(x);
    toastHost().appendChild(el);
    var dur = opts.duration != null ? opts.duration : (type === "error" ? 6000 : 4000);
    if (dur > 0) timer = setTimeout(close, dur);
    return close;
  };

  // ── modal core ───────────────────────────────────────────────────────────
  var MODAL_IC = { info: "ℹ️", success: "✅", warn: "⚠️", warning: "⚠️", error: "⛔", question: "❓" };
  function openModal(cfg) {
    // cfg: {title, message, type, icon, buttons:[{text,value,style,default,cancel}], input}
    return new Promise(function (resolve) {
      var prevFocus = document.activeElement;
      var bg = document.createElement("div");
      bg.className = "jlui-modal-bg";
      var modal = document.createElement("div");
      modal.className = "jlui-modal";
      modal.setAttribute("role", cfg.input ? "dialog" : "alertdialog");
      modal.setAttribute("aria-modal", "true");
      var titleId = "jlui-mt-" + Math.floor(performance.now());
      var msgId = "jlui-mm-" + Math.floor(performance.now());
      modal.setAttribute("aria-labelledby", titleId);
      modal.setAttribute("aria-describedby", msgId);

      var icon = cfg.icon != null ? cfg.icon : (MODAL_IC[cfg.type] || "");
      var html = "";
      if (icon) html += '<div class="jlui-modal-ic" aria-hidden="true"></div>';
      html += '<h2 class="jlui-modal-title" id="' + titleId + '"></h2>';
      html += '<div class="jlui-modal-msg" id="' + msgId + '"></div>';
      if (cfg.input) {
        html += '<input class="jlui-modal-in" />';
        html += '<div class="jlui-modal-err" aria-live="polite"></div>';
      }
      html += '<div class="jlui-modal-btns"></div>';
      modal.innerHTML = html;
      if (icon) modal.querySelector(".jlui-modal-ic").textContent = icon;
      modal.querySelector(".jlui-modal-title").textContent = cfg.title || "";
      modal.querySelector(".jlui-modal-msg").textContent = cfg.message || "";

      var input = null, errEl = null;
      if (cfg.input) {
        input = modal.querySelector(".jlui-modal-in");
        errEl = modal.querySelector(".jlui-modal-err");
        input.type = cfg.input.inputType || "text";
        if (cfg.input.placeholder) input.placeholder = cfg.input.placeholder;
        if (cfg.input.defaultValue) input.value = cfg.input.defaultValue;
      }

      var closed = false;
      function finish(val) {
        if (closed) return; closed = true;
        document.removeEventListener("keydown", onKey, true);
        bg.style.opacity = "0"; bg.style.transition = "opacity .15s";
        setTimeout(function () { bg.remove(); }, 150);
        if (prevFocus && prevFocus.focus) { try { prevFocus.focus(); } catch (_) {} }
        resolve(val);
      }
      var cancelValue = (cfg.buttons.find(function (b) { return b.cancel; }) || {}).value;

      var btnRow = modal.querySelector(".jlui-modal-btns");
      cfg.buttons.forEach(function (b) {
        var btn = document.createElement("button");
        btn.type = "button";
        btn.className = "jlui-btn " + (b.style === "danger" ? "jlui-btn-danger" : b.style === "ghost" ? "jlui-btn-ghost" : "jlui-btn-primary");
        btn.textContent = b.text;
        btn.addEventListener("click", function () {
          if (b.validate && input) {
            var msg = b.validate(input.value);
            if (msg) { errEl.textContent = msg; input.classList.add("jlui-invalid"); input.focus(); return; }
          }
          finish(input && !b.cancel ? input.value : b.value);
        });
        btnRow.appendChild(btn);
        if (b.default) btn._default = true;
      });

      function onKey(e) {
        if (e.key === "Escape") { e.preventDefault(); finish(cancelValue); }
        else if (e.key === "Enter" && !cfg.input) {
          var def = btnRow.querySelector("button"); // primary/default
          var d = Array.prototype.find.call(btnRow.children, function (c) { return c._default; }) || def;
          if (d) { e.preventDefault(); d.click(); }
        } else if (e.key === "Enter" && cfg.input && document.activeElement === input) {
          var d2 = Array.prototype.find.call(btnRow.children, function (c) { return c._default; });
          if (d2) { e.preventDefault(); d2.click(); }
        } else { trapFocus(modal, e); }
      }
      document.addEventListener("keydown", onKey, true);
      bg.addEventListener("mousedown", function (e) { if (e.target === bg && cancelValue !== undefined) finish(cancelValue); });

      bg.appendChild(modal);
      document.body.appendChild(bg);
      // focus: input if present, else the default/primary button
      setTimeout(function () {
        if (input) input.focus();
        else {
          var d = Array.prototype.find.call(btnRow.children, function (c) { return c._default; }) || btnRow.querySelector("button");
          if (d) d.focus();
        }
      }, 30);
    });
  }

  window.jlAlert = function (message, opts) {
    opts = opts || {};
    return openModal({
      title: opts.title || "Notice", message: message, type: opts.type || "info",
      buttons: [{ text: opts.okText || "OK", style: "primary", value: true, default: true }],
    });
  };

  window.jlConfirm = function (message, opts) {
    opts = opts || {};
    return openModal({
      title: opts.title || "Please confirm", message: message, type: opts.type || (opts.danger ? "warn" : "question"),
      buttons: [
        { text: opts.cancelText || "Cancel", style: "ghost", value: false, cancel: true },
        { text: opts.confirmText || "Confirm", style: opts.danger ? "danger" : "primary", value: true, default: true },
      ],
    });
  };

  window.jlPrompt = function (message, opts) {
    opts = opts || {};
    var required = opts.required !== false;
    return openModal({
      title: opts.title || "", message: message, type: opts.type || "question",
      input: { inputType: opts.inputType || "text", placeholder: opts.placeholder, defaultValue: opts.defaultValue },
      buttons: [
        { text: opts.cancelText || "Cancel", style: "ghost", value: null, cancel: true },
        {
          text: opts.okText || "OK", style: "primary", default: true,
          validate: function (v) {
            if (required && !String(v || "").trim()) return opts.requiredMsg || "This field is required.";
            if (opts.validate) return opts.validate(v) || null;
            return null;
          },
        },
      ],
    });
  };

  // ── form validation helpers ───────────────────────────────────────────────
  function errNode(input) {
    var n = input.nextElementSibling;
    if (n && n.classList && n.classList.contains("jlui-fielderr")) return n;
    n = document.createElement("div"); n.className = "jlui-fielderr"; n.setAttribute("aria-live", "polite");
    input.insertAdjacentElement("afterend", n);
    return n;
  }
  window.jlSetError = function (input, msg) {
    if (!input) return;
    input.classList.add("jlui-invalid");
    input.setAttribute("aria-invalid", "true");
    var n = errNode(input); n.textContent = msg || "Invalid value";
    if (!input.id) input.id = "jlf-" + Math.floor(performance.now() + Math.random() * 1000);
    input.setAttribute("aria-describedby", (n.id = n.id || input.id + "-err"));
  };
  window.jlClearError = function (input) {
    if (!input) return;
    input.classList.remove("jlui-invalid");
    input.removeAttribute("aria-invalid");
    var n = input.nextElementSibling;
    if (n && n.classList && n.classList.contains("jlui-fielderr")) n.textContent = "";
  };
  window.jlClearErrors = function (form) {
    if (!form) return;
    form.querySelectorAll(".jlui-invalid").forEach(function (i) { jlClearError(i); });
  };

  // validator factory: each returns (value,input)->errorString|null
  window.jlV = {
    required: function (msg) { return function (v) { return String(v == null ? "" : v).trim() ? null : (msg || "This field is required."); }; },
    email: function (msg) { return function (v) { if (!v) return null; return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim()) ? null : (msg || "Enter a valid email address."); }; },
    minLen: function (n, msg) { return function (v) { return String(v || "").length >= n ? null : (msg || ("Must be at least " + n + " characters.")); }; },
    maxLen: function (n, msg) { return function (v) { return String(v || "").length <= n ? null : (msg || ("Must be at most " + n + " characters.")); }; },
    pattern: function (re, msg) { return function (v) { if (!v) return null; return re.test(v) ? null : (msg || "Invalid format."); }; },
    phoneIN: function (msg) { return function (v) { if (!v) return null; return /^[6-9]\d{9}$/.test(String(v).replace(/\D/g, "").replace(/^91(?=\d{10}$)/, "")) ? null : (msg || "Enter a valid 10-digit mobile number."); }; },
    pincodeIN: function (msg) { return function (v) { if (!v) return null; return /^\d{6}$/.test(String(v).replace(/\D/g, "")) ? null : (msg || "Enter a valid 6-digit pincode."); }; },
    strongPassword: function (msg) { return function (v) { if (!v) return null; return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,}$/.test(v) ? null : (msg || "Min 8 chars incl. upper, lower, number & symbol."); }; },
    match: function (getOther, msg) { return function (v) { var other = typeof getOther === "function" ? getOther() : getOther; return v === other ? null : (msg || "Values do not match."); }; },
    number: function (o, msg) { o = o || {}; return function (v) { if (v === "" || v == null) return null; var n = Number(v); if (isNaN(n)) return msg || "Enter a valid number."; if (o.min != null && n < o.min) return msg || ("Must be ≥ " + o.min + "."); if (o.max != null && n > o.max) return msg || ("Must be ≤ " + o.max + "."); return null; }; },
    // Letters, spaces and name punctuation only (Unicode-aware: accepts any language's letters).
    name: function (msg) { return function (v) { if (!v) return null; return /^[\p{L}\p{M}][\p{L}\p{M}\s.'-]*$/u.test(String(v).trim()) ? null : (msg || "Only letters and spaces are allowed."); }; },
    // Brand/category-style: letters, digits, spaces and & . - ' (so "3M", "V-Guard", "Bosch Series 6" pass).
    title: function (msg) { return function (v) { if (!v) return null; return /^[\p{L}\p{M}\p{N}][\p{L}\p{M}\p{N}\s.&'-]*$/u.test(String(v).trim()) ? null : (msg || "Use only letters, numbers and spaces."); }; },
    // Whole number (digits only), optional range.
    integer: function (o, msg) { o = o || {}; return function (v) { if (v === "" || v == null) return null; if (!/^\d+$/.test(String(v).trim())) return msg || "Enter a whole number (digits only)."; var n = Number(v); if (o.min != null && n < o.min) return msg || ("Must be ≥ " + o.min + "."); if (o.max != null && n > o.max) return msg || ("Must be ≤ " + o.max + "."); return null; }; },
    // Decimal number, optional range and max decimal places.
    decimal: function (o, msg) { o = o || {}; return function (v) { if (v === "" || v == null) return null; var re = o.maxDecimals != null ? new RegExp("^\\d+(\\.\\d{1," + o.maxDecimals + "})?$") : /^\d+(\.\d+)?$/; if (!re.test(String(v).trim())) return msg || "Enter a valid amount."; var n = Number(v); if (o.min != null && n < o.min) return msg || ("Must be ≥ " + o.min + "."); if (o.max != null && n > o.max) return msg || ("Must be ≤ " + o.max + "."); return null; }; },
    // Alphanumeric code (e.g. coupon code).
    code: function (msg) { return function (v) { if (!v) return null; return /^[A-Za-z0-9]+$/.test(String(v).trim()) ? null : (msg || "Use only letters and numbers."); }; },
  };

  // run a set of validators over a value; return first error or null
  function runRules(value, rules, input) {
    for (var i = 0; i < rules.length; i++) { var e = rules[i](value, input); if (e) return e; }
    return null;
  }

  /** Validate a whole form. rules = { fieldNameOrId: [validator,...] }. Returns {valid, errors:{field:msg}}.
      Looks up each field by [name] then #id within the form. Shows inline errors and focuses the first. */
  window.jlValidate = function (form, rules) {
    var errors = {}, firstBad = null;
    Object.keys(rules).forEach(function (key) {
      var input = form.querySelector('[name="' + key + '"]') || form.querySelector("#" + key);
      if (!input) return;
      var msg = runRules(input.value, rules[key], input);
      if (msg) { errors[key] = msg; jlSetError(input, msg); if (!firstBad) firstBad = input; }
      else jlClearError(input);
    });
    if (firstBad) firstBad.focus();
    return { valid: Object.keys(errors).length === 0, errors: errors };
  };

  /** Attach real-time validation to a single input. Validates on blur, and (after the
      first blur) live on input, clearing the error as soon as it becomes valid. */
  window.jlField = function (input, rules) {
    if (!input) return;
    var touched = false;
    function check() { var m = runRules(input.value, rules, input); if (m) jlSetError(input, m); else jlClearError(input); return !m; }
    input.addEventListener("blur", function () { touched = true; check(); });
    input.addEventListener("input", function () { if (touched) check(); });
    return check;
  };

  /** Map a backend field-error object ({field:msg}) onto a form's inputs. */
  window.jlShowServerErrors = function (form, errors) {
    if (!form || !errors || typeof errors !== "object") return false;
    var shown = false, first = null;
    Object.keys(errors).forEach(function (k) {
      var input = form.querySelector('[name="' + k + '"]') || form.querySelector("#" + k);
      if (input) { jlSetError(input, errors[k]); shown = true; if (!first) first = input; }
    });
    if (first) first.focus();
    return shown;
  };

  // ── content dialog ─────────────────────────────────────────────────────────
  /** Open any element as an accessible modal dialog (focus trap, Esc, backdrop,
      focus restore). Pass an EXISTING element (e.g. a hidden form card) and it's
      moved into the overlay and returned to its place on close, or a freshly-built
      element (rendered then discarded). opts: {title, size:'lg', dismissable, onClose}.
      Returns { close(), content, shell }. */
  window.jlDialog = function (content, opts) {
    opts = opts || {};
    var prevFocus = document.activeElement;
    var hasParent = !!content.parentNode;
    var ph = null, wasHidden = false;
    if (hasParent) {
      ph = document.createComment("jl-dialog");
      content.parentNode.insertBefore(ph, content);
      wasHidden = content.classList.contains("hidden");
      content.classList.remove("hidden");
    }
    var bg = document.createElement("div");
    bg.className = "jlui-modal-bg jlui-dialog-bg";
    var shell = document.createElement("div");
    shell.className = "jlui-dialog-shell" + (opts.size === "lg" ? " lg" : "");
    shell.setAttribute("role", "dialog");
    shell.setAttribute("aria-modal", "true");
    if (opts.title) shell.setAttribute("aria-label", opts.title);
    var x = document.createElement("button");
    x.type = "button"; x.className = "jlui-dialog-x"; x.setAttribute("aria-label", "Close"); x.innerHTML = "&times;";
    shell.appendChild(x); shell.appendChild(content);
    bg.appendChild(shell); document.body.appendChild(bg);
    document.documentElement.style.overflow = "hidden";

    var closed = false;
    function close() {
      if (closed) return; closed = true;
      document.removeEventListener("keydown", onKey, true);
      document.documentElement.style.overflow = "";
      if (hasParent && ph && ph.parentNode) {
        ph.parentNode.insertBefore(content, ph); ph.remove();
        if (wasHidden) content.classList.add("hidden");
      }
      bg.remove();
      if (prevFocus && prevFocus.focus) { try { prevFocus.focus(); } catch (_) {} }
      if (opts.onClose) opts.onClose();
    }
    function onKey(e) { if (e.key === "Escape") { e.preventDefault(); close(); } else trapFocus(shell, e); }
    document.addEventListener("keydown", onKey, true);
    bg.addEventListener("mousedown", function (e) { if (e.target === bg && opts.dismissable !== false) close(); });
    x.addEventListener("click", close);
    setTimeout(function () {
      var f = content.querySelector(FOCUSABLE);
      if (f) f.focus(); else { shell.setAttribute("tabindex", "-1"); shell.focus(); }
    }, 30);
    return { close: close, content: content, shell: shell };
  };

  // ── typed input restriction (block invalid characters at the source) ───────
  // Presets: what each field TYPE is allowed to contain, a cleaner that strips the
  // rest (Unicode-aware — Array.from iterates code points so emoji/surrogates go
  // cleanly), and the message shown when the user tries to enter something invalid.
  var RESTRICT = {
    // Person names, city, state: letters, spaces and . ' - only.
    name: { ok: /[\p{L}\p{M}\s.'-]/u, msg: "Only letters and spaces are allowed.", inputmode: "text" },
    // Brand/category: letters, digits, spaces and & . ' - (keeps "3M", "V-Guard").
    title: { ok: /[\p{L}\p{M}\p{N}\s.&'-]/u, msg: "Use only letters, numbers and spaces.", inputmode: "text" },
    // Whole numbers only (phone, pincode, quantity, stock, OTP, usage limits, age).
    digits: { ok: /[0-9]/, msg: "Only numbers are allowed.", inputmode: "numeric" },
    // Money / percentages: digits and a single decimal point.
    decimal: { decimal: true, msg: "Only numbers and a decimal point are allowed.", inputmode: "decimal" },
    // Coupon code etc.: uppercase letters + digits (auto-uppercased).
    code: { ok: /[A-Za-z0-9]/, upper: true, msg: "Only letters and numbers are allowed.", inputmode: "text" },
  };

  function cleanValue(value, type, opts) {
    var cfg = RESTRICT[type] || RESTRICT.name;
    var out;
    if (cfg.decimal) {
      // keep digits + dots, then collapse to a single dot and trim decimal places
      var kept = Array.from(value).filter(function (c) { return /[0-9.]/.test(c); }).join("");
      var parts = kept.split(".");
      out = parts.length > 1 ? parts[0] + "." + parts.slice(1).join("") : kept;
      var md = opts && opts.maxDecimals != null ? opts.maxDecimals : 2;
      var dot = out.indexOf(".");
      if (dot !== -1 && md >= 0) out = out.slice(0, dot + 1 + md);
    } else {
      out = Array.from(value).filter(function (c) { return cfg.ok.test(c); }).join("");
      if (cfg.upper) out = out.toUpperCase();
    }
    if (opts && opts.maxLength != null) out = out.slice(0, opts.maxLength);
    return out;
  }

  /** Restrict an input to a TYPE, filtering invalid characters on typing, paste,
      drag-drop and autofill, preserving the caret, and flashing an inline message
      when something is blocked. Also sets inputmode/maxlength for good mobile UX. */
  window.jlRestrict = function (input, type, opts) {
    if (!input || input.__jlRestrict) return; input.__jlRestrict = true;
    opts = opts || {};
    var cfg = RESTRICT[type] || RESTRICT.name;
    if (cfg.inputmode && !input.getAttribute("inputmode")) input.setAttribute("inputmode", cfg.inputmode);
    if (opts.maxLength != null && !input.getAttribute("maxlength")) input.setAttribute("maxlength", String(opts.maxLength));
    var msgTimer;
    function flash() {
      jlSetError(input, cfg.msg);
      clearTimeout(msgTimer);
      msgTimer = setTimeout(function () { if (!input.classList.contains("jlui-invalid-sticky")) jlClearError(input); }, 2000);
    }
    function sanitize() {
      var before = input.value;
      if (before === "") return;
      var pos = input.selectionStart;
      var cleaned = cleanValue(before, type, opts);
      if (cleaned === before) return;
      // count how many chars before the caret were removed, to keep the caret sensible
      var removedBeforeCaret = Array.from(before.slice(0, pos)).length - Array.from(cleanValue(before.slice(0, pos), type, opts)).length;
      input.value = cleaned;
      try { var np = Math.max(0, (pos || cleaned.length) - removedBeforeCaret); input.setSelectionRange(np, np); } catch (_) {}
      flash();
    }
    // 'input' covers typing, paste and drag-drop; 'change'/'blur' back-stop autofill.
    input.addEventListener("input", sanitize);
    input.addEventListener("change", sanitize);
    input.addEventListener("blur", sanitize);
    // some browsers fire autofill without input — sweep shortly after load
    setTimeout(sanitize, 400);
  };

  /** Scan for inputs with a data-jl="type" attribute and restrict them. Optional
      data-jl-maxlength / data-jl-decimals refine the rule. Idempotent; also runs
      automatically on DOMContentLoaded, and can be re-run for injected forms. */
  window.jlAutoRestrict = function (root) {
    (root || document).querySelectorAll("[data-jl]").forEach(function (el) {
      var opts = {};
      if (el.dataset.jlMaxlength) opts.maxLength = +el.dataset.jlMaxlength;
      if (el.dataset.jlDecimals) opts.maxDecimals = +el.dataset.jlDecimals;
      jlRestrict(el, el.dataset.jl, opts);
    });
  };
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", function () { jlAutoRestrict(); });
  else jlAutoRestrict();
})();
