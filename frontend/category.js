/* ══════════════════════════════════════════════════════════════════════
   Shared category-listing controller. Each category page sets
   window.JL_CATEGORY = "<slug>" then includes store.js + this file.
   It fetches that category's products from the API and drives the existing
   filter/sort UI (#fPrice #fBrand #fRating #fSort #fClear #grid #resultCount).
   ══════════════════════════════════════════════════════════════════════ */
(function () {
  const slug = window.JL_CATEGORY;
  const grid = document.getElementById("grid");
  if (!slug || !grid) return;

  const fPrice = document.getElementById("fPrice");
  const fBrand = document.getElementById("fBrand");
  const fRating = document.getElementById("fRating");
  const fSort = document.getElementById("fSort");
  const fClear = document.getElementById("fClear");
  const resultCount = document.getElementById("resultCount");
  let all = [];

  function render() {
    let list = [...all];
    if (fPrice && fPrice.value) {
      const [min, max] = fPrice.value.split("-");
      if (min) list = list.filter((p) => Number(p.price) >= Number(min));
      if (max) list = list.filter((p) => Number(p.price) <= Number(max));
    }
    if (fBrand && fBrand.value) list = list.filter((p) => (p.brandName || "") === fBrand.value);
    if (fRating && fRating.value) list = list.filter((p) => Number(p.averageRating || 0) >= Number(fRating.value));
    if (fSort) {
      switch (fSort.value) {
        case "price_asc": list.sort((a, b) => a.price - b.price); break;
        case "price_desc": list.sort((a, b) => b.price - a.price); break;
        case "rating": list.sort((a, b) => Number(b.averageRating || 0) - Number(a.averageRating || 0)); break;
        case "discount": list.sort((a, b) => jlPctOff(b.comparePrice, b.price) - jlPctOff(a.comparePrice, a.price)); break;
      }
    }
    grid.innerHTML = list.length
      ? list.map(jlProductCard).join("")
      : `<div class="col-span-full text-center py-16 text-slate-400">No products in this category yet.</div>`;
    if (resultCount) resultCount.textContent = `${list.length} of ${all.length} products`;
    if (fClear) {
      const active = (fPrice && fPrice.value) || (fBrand && fBrand.value) || (fRating && fRating.value) || (fSort && fSort.value);
      fClear.classList.toggle("hidden", !active);
    }
  }

  async function boot() {
    grid.innerHTML = `<div class="col-span-full text-center py-12">${typeof jlSpinnerHTML === "function" ? jlSpinnerHTML({label:"Loading products"}) : '<span class="text-slate-400">Loading…</span>'}</div>`;
    try {
      const page = await JLStore.products({ category: slug, size: 100 });
      all = (page && page.content) || [];
      if (fBrand) {
        const brands = [...new Set(all.map((p) => p.brandName).filter(Boolean))].sort();
        fBrand.innerHTML = '<option value="">All brands</option>' + brands.map((b) => `<option value="${jlEsc(b)}">${jlEsc(b)}</option>`).join("");
      }
      render();
    } catch (e) {
      grid.innerHTML = `<div class="col-span-full text-center py-12 text-red-500">Couldn't load products. Please refresh shortly.</div>`;
    }
  }

  [fPrice, fBrand, fRating, fSort].forEach((s) => s && s.addEventListener("change", render));
  if (fClear) fClear.addEventListener("click", () => {
    [fPrice, fBrand, fRating, fSort].forEach((s) => { if (s) s.value = ""; });
    render();
  });
  boot();
})();
