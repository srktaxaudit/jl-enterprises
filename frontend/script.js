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

/* ── 2. Add to Cart buttons ──────────────────────────────────────── */
// Any button with class="addbtn" and data-id / data-name / data-brand /
// data-emoji / data-price attributes adds that product to the cart.

document.addEventListener("click", (e) => {
  const btn = e.target.closest(".addbtn");
  if (!btn) return;

  const cart = getCart();
  const existing = cart.find((item) => item.id === btn.dataset.id);
  if (existing) {
    existing.qty += 1; // already in cart → just bump the quantity
  } else {
    cart.push({
      id: btn.dataset.id,
      name: btn.dataset.name,
      brand: btn.dataset.brand || "",
      emoji: btn.dataset.emoji || "📦",
      price: Number(btn.dataset.price),
      qty: 1,
    });
  }
  saveCart(cart);

  // quick visual feedback on the button
  const oldText = btn.textContent;
  btn.textContent = "✔ Added!";
  setTimeout(() => (btn.textContent = oldText), 900);
});

/* ── 4. Cart page (cart.html) ────────────────────────────────────── */

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
        <div class="cart-thumb">${item.emoji}</div>
        <div class="cart-info">
          <div class="brand">${item.brand}</div>
          <div class="name">${item.name}</div>
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
}

// ✕ Remove button
function removeItem(index) {
  const cart = getCart();
  cart.splice(index, 1);
  saveCart(cart);
  renderCartPage();
}

// Checkout button — demo only: clears the cart and shows a success box
function checkoutCart() {
  if (getCart().length === 0) return;
  saveCart([]); // empty the cart
  const layoutEl = document.getElementById("cartLayout");
  const doneEl = document.getElementById("cartDone");
  if (layoutEl) layoutEl.style.display = "none";
  if (doneEl) doneEl.classList.add("show");
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
  if (serviceForm) {
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

  // — Sign Up form —
  const signupForm = document.getElementById("signupForm");
  if (signupForm) {
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
  if (loginForm) {
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
  if (!form) return;

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

    const balance = amount - down;
    const monthly = Math.ceil(balance / months);

    document.getElementById("emiBalance").textContent = inrFmt(balance);
    document.getElementById("emiMonthly").textContent = inrFmt(monthly) + " × " + months + " months";
    document.getElementById("emiResult").style.display = "block";
  });
}

/* ── Boot: run once the page is ready ────────────────────────────── */
document.addEventListener("DOMContentLoaded", () => {
  updateCartWidgets();  // badge reflects the stored cart on every page
  renderCartPage();     // only does something on cart.html
  initSearch();         // only does something where a search box exists
  initForms();          // only does something where a form exists
  initTrackOrder();     // only does something on track-order.html
  initEmiCalculator();  // only does something on emi-payment.html
});
