/* ════════════════════════════════════════════════════════════════════
   JL ENTERPRISES — shared JavaScript for all static pages.

   What lives here:
     1. Cart stored in localStorage (works across every page)
     2. "Add to Cart" button handling (product grids)
     3. Header badge + floating cart updater
     4. Cart page rendering (quantity +/−, remove, totals)
     5. Search filter for the product cards on index.html
     6. Form handling: service booking, sign up, login
        (front-end only for now — shows a success message, no backend)

   Every section checks whether its elements exist first, so this one
   file can be included on every page safely.
   ════════════════════════════════════════════════════════════════════ */

/* ── Helpers ─────────────────────────────────────────────────────── */

// Format a number as Indian Rupees, e.g. 34990 → "₹34,990"
function inrFmt(n) {
  return "₹" + Math.round(n).toLocaleString("en-IN");
}

const CART_KEY = "jl_cart"; // localStorage key shared by all pages

// Read the cart (array of {id, name, brand, emoji, price, qty})
function getCart() {
  try {
    return JSON.parse(localStorage.getItem(CART_KEY)) || [];
  } catch {
    return [];
  }
}

// Save the cart and refresh the header badge / floating cart
function saveCart(cart) {
  localStorage.setItem(CART_KEY, JSON.stringify(cart));
  updateCartWidgets();
}

/* ── 1+3. Header badge and floating cart ─────────────────────────── */

function updateCartWidgets() {
  const cart = getCart();
  const count = cart.reduce((sum, item) => sum + item.qty, 0);
  const total = cart.reduce((sum, item) => sum + item.price * item.qty, 0);

  // small orange badge on the 🛒 icon in the header
  const badge = document.getElementById("cartBadge");
  if (badge) {
    badge.textContent = count;
    badge.classList.toggle("hidden", count === 0);
    badge.classList.toggle("flex", count > 0);
  }

  // floating "x items · ₹total" pill (bottom-right on some pages)
  const fc = document.getElementById("floatingCart");
  if (fc) {
    fc.classList.toggle("hidden", count === 0);
    fc.classList.toggle("flex", count > 0);
    const fCount = document.getElementById("floatCount");
    const fTotal = document.getElementById("floatTotal");
    if (fCount) fCount.textContent = count;
    if (fTotal) fTotal.textContent = inrFmt(total);
  }
}

/* ── 2. Add to Cart → persistent quantity stepper ─────────────────── */
// Every place a product can be added renders a `.jlcart` control (via
// jlCartControl below). When the product is NOT in the cart it shows an
// "Add to Cart" button; once added it becomes a −/qty/+ stepper that reflects
// the live cart quantity. Because the cart lives in localStorage, the stepper
// stays correct across refresh, navigation and returning to a listing. Reaching
// quantity 0 removes the product and reverts to the "Add to Cart" button.
//
// The whole thing is event-delegated + driven off localStorage, so it is
// synchronous and instant — there are no per-change network calls to de-dupe
// (the cart is mirrored to the backend once, at checkout, as before).

// Current quantity of a product in the cart (0 if absent).
function jlCartQtyOf(id) {
  const item = getCart().find((x) => String(x.id) === String(id));
  return item ? item.qty : 0;
}

// Inner markup for a control given the current quantity. `variant` is "card"
// (full-width, on product cards) or "detail" (inline, on the product page).
function jlCartControlInner(qty, stock, variant) {
  const detail = variant === "detail";
  if (qty <= 0) {
    const cls = detail
      ? "jlcart-add bg-navy hover:bg-orange text-white font-bold px-6 py-3 rounded-lg text-sm transition"
      : "jlcart-add w-full bg-navy hover:bg-orange text-white font-bold py-2.5 rounded-lg text-sm transition";
    return `<button type="button" class="${cls}">🛒 Add to Cart</button>`;
  }
  const atMax = stock !== "" && stock != null && qty >= Number(stock);
  const wrapCls = detail
    ? "jlcart-stepper inline-flex items-center bg-navy text-white rounded-lg overflow-hidden select-none"
    : "jlcart-stepper w-full flex items-center justify-between bg-navy text-white rounded-lg overflow-hidden select-none";
  const pad = detail ? "px-5 py-3" : "px-4 py-2.5";
  return `<div class="${wrapCls}">
      <button type="button" class="jlcart-dec ${pad} text-lg leading-none hover:bg-orange transition" aria-label="Decrease quantity">−</button>
      <span class="jlcart-count font-bold text-sm px-3" aria-live="polite">${qty}</span>
      <button type="button" class="jlcart-inc ${pad} text-lg leading-none hover:bg-orange transition${atMax ? " opacity-50 cursor-not-allowed" : ""}" aria-label="Increase quantity"${atMax ? " disabled" : ""}>+</button>
    </div>`;
}

// Build a full cart control for a product. `p` = {id, name, brand, emoji, price, stock}.
// `opts.variant` = "card" (default) | "detail". Renders the correct state on first
// paint by reading the live cart, so no flash and no separate sync pass is needed.
function jlCartControl(p, opts) {
  opts = opts || {};
  const variant = opts.variant || "card";
  const id = String(p.id);
  const stock = (p.stock == null || p.stock === "") ? "" : Number(p.stock);
  const data =
    `data-id="${escHtml(id)}" data-name="${escHtml(p.name)}" data-brand="${escHtml(p.brand || "")}" ` +
    `data-emoji="${escHtml(p.emoji || "📦")}" data-price="${Number(p.price) || 0}" ` +
    `data-stock="${stock}" data-variant="${variant}"`;
  return `<div class="jlcart" ${data}>${jlCartControlInner(jlCartQtyOf(id), stock, variant)}</div>`;
}

// Re-render every on-page control (optionally just those for one product) so all
// placements of the same product stay in sync after a change.
function jlSyncCartControls(onlyId) {
  document.querySelectorAll(".jlcart").forEach((wrap) => {
    if (onlyId != null && String(wrap.dataset.id) !== String(onlyId)) return;
    const stockRaw = wrap.dataset.stock;
    const stock = (stockRaw === "" || stockRaw == null) ? "" : Number(stockRaw);
    wrap.innerHTML = jlCartControlInner(jlCartQtyOf(wrap.dataset.id), stock, wrap.dataset.variant || "card");
  });
}

// Apply a +1 / −1 change from a control, enforcing stock and removing at 0.
function jlCartBump(wrap, delta) {
  const id = wrap.dataset.id;
  const stockRaw = wrap.dataset.stock;
  const stock = (stockRaw === "" || stockRaw == null) ? null : Number(stockRaw);
  const cur = jlCartQtyOf(id);
  let next = cur + delta;
  if (delta > 0 && stock != null && next > stock) {
    if (typeof jlToast === "function") jlToast(`Only ${stock} in stock.`, { type: "warn" });
    return;
  }
  if (next < 0) next = 0;

  const cart = getCart();
  const idx = cart.findIndex((x) => String(x.id) === String(id));
  if (next <= 0) {
    if (idx >= 0) cart.splice(idx, 1);
  } else if (idx >= 0) {
    cart[idx].qty = next;
  } else {
    cart.push({
      id,
      name: wrap.dataset.name,
      brand: wrap.dataset.brand || "",
      emoji: wrap.dataset.emoji || "📦",
      price: Number(wrap.dataset.price) || 0,
      qty: next,
    });
  }
  saveCart(cart);            // refreshes header badge + floating cart
  jlSyncCartControls(id);    // update every placement of this product
  renderCartPage();          // refresh the cart page if we're on it
}

// One delegated handler covers every control on every page (including cards
// rendered later by the API). Disabled +/− buttons don't emit clicks.
document.addEventListener("click", (e) => {
  const wrap = e.target.closest(".jlcart");
  if (!wrap) return;
  if (e.target.closest(".jlcart-add") || e.target.closest(".jlcart-inc")) jlCartBump(wrap, +1);
  else if (e.target.closest(".jlcart-dec")) jlCartBump(wrap, -1);
});

/* ── 4. Cart page (cart.html) ────────────────────────────────────── */

// Escape user/product-controlled strings before injecting them as HTML, so a
// value like `<img onerror=...>` can never execute (DOM-XSS protection).
function escHtml(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function renderCartPage() {
  const listEl = document.getElementById("cartItems");
  if (!listEl) return; // not on the cart page

  const emptyEl = document.getElementById("cartEmpty");
  const layoutEl = document.getElementById("cartLayout");
  const cart = getCart();

  // empty cart → show the "Your cart is empty" block instead
  if (cart.length === 0) {
    if (emptyEl) emptyEl.style.display = "block";
    if (layoutEl) layoutEl.style.display = "none";
    return;
  }
  if (emptyEl) emptyEl.style.display = "none";
  if (layoutEl) layoutEl.style.display = "";

  // one .cart-item row per product
  listEl.innerHTML = cart
    .map(
      (item, i) => `
      <div class="cart-item">
        <div class="cart-thumb">${escHtml(item.emoji)}</div>
        <div class="cart-info">
          <div class="brand">${escHtml(item.brand)}</div>
          <div class="name">${escHtml(item.name)}</div>
          <div class="price">${inrFmt(item.price)}</div>
        </div>
        <div class="qty-box">
          <button type="button" onclick="changeQty(${i}, -1)" aria-label="Decrease quantity">−</button>
          <span class="qty">${item.qty}</span>
          <button type="button" onclick="changeQty(${i}, 1)" aria-label="Increase quantity">+</button>
        </div>
        <button type="button" class="remove-btn" onclick="removeItem(${i})">✕ Remove</button>
      </div>`
    )
    .join("");

  // totals card
  const subtotal = cart.reduce((sum, item) => sum + item.price * item.qty, 0);
  const count = cart.reduce((sum, item) => sum + item.qty, 0);
  const subEl = document.getElementById("cartSubtotal");
  const totEl = document.getElementById("cartTotal");
  const cntEl = document.getElementById("cartCount");
  if (subEl) subEl.textContent = inrFmt(subtotal);
  if (totEl) totEl.textContent = inrFmt(subtotal); // delivery is FREE
  if (cntEl) cntEl.textContent = count;
}

// + / − buttons (removing when quantity drops to 0)
function changeQty(index, delta) {
  const cart = getCart();
  if (!cart[index]) return;
  cart[index].qty += delta;
  if (cart[index].qty <= 0) cart.splice(index, 1);
  saveCart(cart);
  renderCartPage();
  jlSyncCartControls(); // keep any product-card steppers on this page in sync
}

// ✕ Remove button
function removeItem(index) {
  const cart = getCart();
  cart.splice(index, 1);
  saveCart(cart);
  renderCartPage();
  jlSyncCartControls();
}

/* ── 5. Search filter on index.html ──────────────────────────────── */
// Typing in the header search box hides product cards that don't match.

function applySearch(term) {
  const cards = document.querySelectorAll(".product-card");
  if (!cards.length) return;
  const q = term.trim().toLowerCase();
  let visible = 0;
  cards.forEach((card) => {
    const match = !q || (card.dataset.search || "").includes(q);
    card.style.display = match ? "" : "none";
    if (match) visible++;
  });
  // "no results" message
  const none = document.getElementById("noResults");
  if (none) none.style.display = visible === 0 ? "block" : "none";
}

function initSearch() {
  const input = document.getElementById("searchInput");
  if (!input) return;

  // live filtering as you type (only does something on index.html,
  // where .product-card elements exist)
  input.addEventListener("input", () => applySearch(input.value));

  // if we arrived here as index.html?q=... (search submitted from
  // another page), apply that term straight away
  const q = new URLSearchParams(location.search).get("q");
  if (q && document.querySelector(".product-card")) {
    input.value = q;
    applySearch(q);
    const grid = document.getElementById("products");
    if (grid) grid.scrollIntoView({ behavior: "smooth" });
  }
}

/* ── 6. Forms (service / signup / login) ─────────────────────────── */

// Small helpers for validation feedback
function markInvalid(input, hintId) {
  input.classList.add("invalid");
  const hint = hintId && document.getElementById(hintId);
  if (hint) hint.classList.add("show");
}
function clearInvalid(form) {
  form.querySelectorAll(".invalid").forEach((el) => el.classList.remove("invalid"));
  form.querySelectorAll(".hint.show").forEach((el) => el.classList.remove("show"));
}
// Indian mobile: exactly 10 digits (spaces / +91 stripped first)
function isValidMobile(value) {
  const digits = value.replace(/\D/g, "").replace(/^91(?=\d{10}$)/, "");
  return /^[6-9]\d{9}$/.test(digits);
}

// Generic: hide the form card's form + show its success box
function showSuccess(form, successId) {
  form.style.display = "none";
  const links = form.parentElement.querySelector(".form-links");
  if (links) links.style.display = "none";
  document.getElementById(successId).classList.add("show");
}

function initForms() {
  // — Service booking form —
  const serviceForm = document.getElementById("serviceForm");
  if (serviceForm && !window.JL_BACKEND_SERVICE) {
    serviceForm.addEventListener("submit", (e) => {
      e.preventDefault();
      clearInvalid(serviceForm);
      const mobile = document.getElementById("svcMobile");
      if (!isValidMobile(mobile.value)) {
        markInvalid(mobile, "svcMobileHint");
        return;
      }
      // fill the booking summary into the success message
      const name = document.getElementById("svcName").value.trim();
      const el = document.getElementById("svcSummary");
      if (el) el.textContent = `Thanks ${name}! Our service team will call you within 1 working day to confirm the visit.`;
      showSuccess(serviceForm, "serviceSuccess");
    });
  }

  // Pages that wire sign up / login to the real backend set this flag
  // (see signup.html / login.html) so the demo handlers below stand down.
  const backendAuth = window.JL_BACKEND_AUTH === true;

  // — Sign Up form —
  const signupForm = document.getElementById("signupForm");
  if (signupForm && !backendAuth) {
    signupForm.addEventListener("submit", (e) => {
      e.preventDefault();
      clearInvalid(signupForm);

      const mobile = document.getElementById("suMobile");
      const pass = document.getElementById("suPassword");
      const confirm = document.getElementById("suConfirm");
      let ok = true;

      // rule 1: mobile number must be a valid 10-digit number
      if (!isValidMobile(mobile.value)) {
        markInvalid(mobile, "suMobileHint");
        ok = false;
      }
      // rule 2: password and confirm password must match
      if (pass.value.length < 6) {
        markInvalid(pass, "suPassHint");
        ok = false;
      } else if (pass.value !== confirm.value) {
        markInvalid(confirm, "suConfirmHint");
        ok = false;
      }
      if (!ok) return;

      showSuccess(signupForm, "signupSuccess");
    });
  }

  // — Login form —
  const loginForm = document.getElementById("loginForm");
  if (loginForm && !backendAuth) {
    loginForm.addEventListener("submit", (e) => {
      e.preventDefault();
      showSuccess(loginForm, "loginSuccess");
    });
  }
}

/* ── 7. Track Order (track-order.html) ───────────────────────────── */
// Shows a sample 4-step status after clicking "Track Order".
// (Real tracking is connected to the database in the JL Store app.)

function initTrackOrder() {
  const form = document.getElementById("trackForm");
  if (!form || window.JL_BACKEND_TRACK) return;   // real tracking wired in track-order.html

  const STEPS = [
    { label: "Order Received", emoji: "🧾" },
    { label: "Packed", emoji: "📦" },
    { label: "Out for Delivery", emoji: "🚚" },
    { label: "Delivered", emoji: "✅" },
  ];

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    clearInvalid(form);

    const orderId = document.getElementById("toOrderId").value.trim().toUpperCase();
    const mobile = document.getElementById("toMobile");
    if (!isValidMobile(mobile.value)) {
      markInvalid(mobile, "toMobileHint");
      return;
    }

    // Sample status: the order is currently "Out for Delivery" (step 3 of 4)
    const currentStep = 3;
    document.getElementById("trackOrderNo").textContent = "#" + (orderId || "JL123456");
    document.getElementById("trackSteps").innerHTML = STEPS.map((s, i) => {
      const done = i < currentStep;
      return `
        <div style="display:flex;align-items:center;gap:12px;padding:9px 0;border-bottom:1px dashed #e2e8f0">
          <span style="width:34px;height:34px;border-radius:50%;display:flex;align-items:center;justify-content:center;
                       font-size:16px;background:${done ? "#dcfce7" : "#f1f5f9"}">${s.emoji}</span>
          <b style="flex:1;font-size:14px;color:${done ? "#0b2447" : "#94a3b8"}">${s.label}</b>
          <span style="font-size:12px;font-weight:700;color:${done ? "#16a34a" : "#cbd5e1"}">
            ${done ? (i === currentStep - 1 ? "● Current" : "✓ Done") : "Pending"}
          </span>
        </div>`;
    }).join("");
    document.getElementById("trackResult").style.display = "block";
  });
}

/* ── 8. EMI Calculator (emi-payment.html) ────────────────────────── */
// Balance = amount − down payment; Monthly EMI = balance ÷ months.

function initEmiCalculator() {
  const form = document.getElementById("emiForm");
  if (!form) return;

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    clearInvalid(form);

    const amountEl = document.getElementById("emiAmount");
    const downEl = document.getElementById("emiDown");
    const amount = Number(amountEl.value);
    const down = Number(downEl.value) || 0;
    const months = Number(document.getElementById("emiMonths").value);

    // validate: amount required, down payment must be smaller than amount
    if (!amount || amount <= 0) {
      markInvalid(amountEl, "emiAmountHint");
      return;
    }
    if (down < 0 || down >= amount) {
      markInvalid(downEl, "emiDownHint");
      return;
    }
    if (!months || months <= 0) {   // guard: never divide by an empty/0 tenure (would show Infinity/NaN)
      markInvalid(document.getElementById("emiMonths"), "emiMonthsHint");
      return;
    }

    const balance = amount - down;
    const monthly = Math.ceil(balance / months);

    document.getElementById("emiBalance").textContent = inrFmt(balance);
    document.getElementById("emiMonthly").textContent = inrFmt(monthly) + " × " + months + " months";
    document.getElementById("emiResult").style.display = "block";
  });
}

/* ── Footer "Contact Us": social + maps links, injected on every page that has the
      site footer (no-ops elsewhere). Kept here so the footer markup stays in one place. */
function initFooterSocials() {
  const container = document.querySelector(".footer-container");
  if (!container || container.querySelector("[data-jl-socials]")) return;

  // Widen the footer grid so the extra column fits (overrides Tailwind's md:grid-cols-4).
  if (!document.getElementById("jl-footer-socials-style")) {
    const st = document.createElement("style");
    st.id = "jl-footer-socials-style";
    st.textContent =
      "@media(min-width:768px){footer .footer-container{grid-template-columns:repeat(auto-fit,minmax(150px,1fr))}}";
    document.head.appendChild(st);
  }

  const col = document.createElement("div");
  col.className = "footer-column";
  col.setAttribute("data-jl-socials", "");
  col.innerHTML =
    "<h3>Contact Us</h3>" +
    '<a href="https://wa.me/919514970111" target="_blank" rel="noopener">💬 WhatsApp</a>' +
    '<a href="https://www.instagram.com/jl_enterprises_tut" target="_blank" rel="noopener">📸 Instagram</a>' +
    '<a href="https://www.facebook.com/people/JL-Enterprisess-Thoothukudi/61557628604440/" target="_blank" rel="noopener">👍 Facebook</a>' +
    '<a href="https://share.google/tUlE8yNjaUiIqf05m" target="_blank" rel="noopener">📍 Find us on Maps</a>';
  container.appendChild(col);
}

/* ── Category nav: append the newer departments to the top category bar on every
      page that shows it (kept here so the bar's markup stays in one place). ── */
function initCategoryNav() {
  const bar = document.querySelector("nav.bg-navy-600 > div");
  if (!bar || bar.querySelector("[data-jl-cat-extra]")) return;
  const EXTRA = [["fans.html", "Fans"], ["air-coolers.html", "Air Coolers"],
                 ["stabilizers.html", "Stabilizers"], ["water-heaters.html", "Water Heaters"]];
  const current = location.pathname.split("/").pop();
  EXTRA.forEach(([href, label]) => {
    const active = href === current;
    const a = document.createElement("a");
    a.href = href;
    a.setAttribute("data-jl-cat-extra", "");
    a.className = active
      ? "text-white px-4 py-2.5 text-sm whitespace-nowrap border-b-[3px] border-orange font-semibold"
      : "text-blue-100/90 hover:text-white px-4 py-2.5 text-sm whitespace-nowrap";
    a.textContent = label;
    bar.appendChild(a);
  });
}

/* ── Boot: run once the page is ready ────────────────────────────── */
document.addEventListener("DOMContentLoaded", () => {
  initCategoryNav();    // appends Fans / Air Coolers / Stabilizers / Water Heaters to the nav bar
  updateCartWidgets();  // badge reflects the stored cart on every page
  jlSyncCartControls(); // any add/qty controls already in the DOM reflect the cart
  renderCartPage();     // only does something on cart.html
  initSearch();         // only does something where a search box exists
  initForms();          // only does something where a form exists
  initTrackOrder();     // only does something on track-order.html
  initEmiCalculator();  // only does something on emi-payment.html
  initFooterSocials();  // adds the "Contact Us" (social) footer column
});
