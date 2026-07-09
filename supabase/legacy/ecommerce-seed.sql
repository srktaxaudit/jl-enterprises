-- ════════════════════════════════════════════════════════════════════════
--  JL E-COMMERCE BACKEND — sample data (categories, brands, products, stock).
--  Run AFTER ecommerce-schema.sql, in Supabase → SQL Editor → Run.
--  Safe to re-run: every insert uses ON CONFLICT ... DO NOTHING.
--
--  IDs use gen_random_uuid() (built into Postgres 13+, no extension needed).
--  Foreign keys are resolved by slug via scalar subqueries.
-- ════════════════════════════════════════════════════════════════════════

-- ── Categories ────────────────────────────────────────────────────────────
insert into categories (id, created_at, updated_at, name, slug, sort_order) values
  (gen_random_uuid(), now(), now(), 'Air Conditioners', 'air-conditioners', 1),
  (gen_random_uuid(), now(), now(), 'Televisions',      'televisions',      2),
  (gen_random_uuid(), now(), now(), 'Refrigerators',    'refrigerators',    3),
  (gen_random_uuid(), now(), now(), 'Washing Machines', 'washing-machines', 4),
  (gen_random_uuid(), now(), now(), 'Home Theatre',     'home-theatre',     5),
  (gen_random_uuid(), now(), now(), 'Kitchen & Stove',  'kitchen',          6),
  (gen_random_uuid(), now(), now(), 'Furniture',        'furniture',        7)
on conflict (slug) do nothing;

-- ── Brands ────────────────────────────────────────────────────────────────
insert into brands (id, created_at, updated_at, name, slug) values
  (gen_random_uuid(), now(), now(), 'Voltas',    'voltas'),
  (gen_random_uuid(), now(), now(), 'Sony',      'sony'),
  (gen_random_uuid(), now(), now(), 'LG',        'lg'),
  (gen_random_uuid(), now(), now(), 'Samsung',   'samsung'),
  (gen_random_uuid(), now(), now(), 'Godrej',    'godrej'),
  (gen_random_uuid(), now(), now(), 'Prestige',  'prestige'),
  (gen_random_uuid(), now(), now(), 'Sleepwell', 'sleepwell')
on conflict (slug) do nothing;

-- ── Products (category_id / brand_id resolved by slug) ─────────────────────
insert into products
  (id, created_at, updated_at, name, slug, sku, short_description, category_id, brand_id,
   price, compare_price, currency, featured, average_rating, review_count)
values
  (gen_random_uuid(), now(), now(), '1.5 Ton 3-Star Inverter Split AC', 'voltas-1-5t-3star-inverter-ac', 'AC-VOLT-15-3',
   'Energy-efficient inverter AC with copper condenser and turbo cooling.',
   (select id from categories where slug='air-conditioners'), (select id from brands where slug='voltas'),
   34990, 51500, 'INR', true, 4.6, 214),

  (gen_random_uuid(), now(), now(), '1 Ton 5-Star Inverter AC', 'lg-1t-5star-inverter-ac', 'AC-LG-10-5',
   'Dual inverter compressor, low noise, 5-star energy rating.',
   (select id from categories where slug='air-conditioners'), (select id from brands where slug='lg'),
   41990, 58000, 'INR', false, 4.5, 98),

  (gen_random_uuid(), now(), now(), '55" 4K Ultra HD Smart Google TV', 'sony-55-4k-google-tv', 'TV-SONY-55-4K',
   'Crisp 4K HDR picture with built-in Google TV and Dolby Audio.',
   (select id from categories where slug='televisions'), (select id from brands where slug='sony'),
   62990, 87900, 'INR', true, 4.4, 176),

  (gen_random_uuid(), now(), now(), '43" 4K Crystal UHD Smart TV', 'samsung-43-4k-tv', 'TV-SAM-43-4K',
   'Crystal processor 4K with smart apps and voice remote.',
   (select id from categories where slug='televisions'), (select id from brands where slug='samsung'),
   32990, 45000, 'INR', false, 4.3, 120),

  (gen_random_uuid(), now(), now(), '340L Double-Door Frost-Free Fridge', 'godrej-340l-double-door-fridge', 'RF-GOD-340',
   'Frost-free double-door refrigerator with inverter compressor.',
   (select id from categories where slug='refrigerators'), (select id from brands where slug='godrej'),
   33990, 42000, 'INR', true, 4.5, 143),

  (gen_random_uuid(), now(), now(), '260L 3-Star Single-Door Fridge', 'lg-260l-fridge', 'RF-LG-260',
   'Spacious single-door fridge with smart inverter and stabilizer-free operation.',
   (select id from categories where slug='refrigerators'), (select id from brands where slug='lg'),
   24990, 30000, 'INR', false, 4.4, 87),

  (gen_random_uuid(), now(), now(), '7kg Fully-Automatic Front Load Washing Machine', 'samsung-7kg-front-load-wm', 'WM-SAM-7',
   'EcoBubble front-load washer with 14 wash programs.',
   (select id from categories where slug='washing-machines'), (select id from brands where slug='samsung'),
   28990, 38000, 'INR', false, 4.3, 65),

  (gen_random_uuid(), now(), now(), '5.1 Channel Home Theatre System', 'sony-51-home-theatre', 'HT-SONY-51',
   'Immersive 5.1 surround sound with Bluetooth and powerful bass.',
   (select id from categories where slug='home-theatre'), (select id from brands where slug='sony'),
   18990, 24000, 'INR', false, 4.2, 54),

  (gen_random_uuid(), now(), now(), '3-Burner Stainless Steel Gas Stove', 'prestige-3burner-gas-stove', 'KT-PRES-3B',
   'Toughened glass top, brass burners, ISI certified.',
   (select id from categories where slug='kitchen'), (select id from brands where slug='prestige'),
   3499, 4500, 'INR', false, 4.4, 210),

  (gen_random_uuid(), now(), now(), '750W Mixer Grinder (3 Jars)', 'prestige-mixer-grinder', 'KT-PRES-MG',
   'Powerful 750W motor with 3 stainless-steel jars.',
   (select id from categories where slug='kitchen'), (select id from brands where slug='prestige'),
   3999, 5500, 'INR', false, 4.5, 320),

  (gen_random_uuid(), now(), now(), 'Queen Orthopedic Memory Foam Mattress', 'sleepwell-queen-mattress', 'FN-SLEEP-Q',
   'Medium-firm memory foam mattress with 10-year warranty.',
   (select id from categories where slug='furniture'), (select id from brands where slug='sleepwell'),
   15990, 22000, 'INR', true, 4.6, 189),

  (gen_random_uuid(), now(), now(), '3-Seater Fabric Sofa', 'jl-3seater-sofa', 'FN-JL-SOFA3',
   'Comfortable 3-seater sofa with solid wood frame and premium fabric.',
   (select id from categories where slug='furniture'), null,
   24990, 32000, 'INR', false, 4.2, 41)
on conflict (slug) do nothing;

-- ── Inventory (one stock row per product; qty resolved by slug) ────────────
--  The 1.5T AC is intentionally low (2 ≤ reorder 3) to demo the low-stock view.
insert into inventories (id, created_at, updated_at, product_id, quantity, reorder_level)
select gen_random_uuid(), now(), now(), p.id, v.qty, v.reorder
from (values
  ('voltas-1-5t-3star-inverter-ac',  2, 3),
  ('lg-1t-5star-inverter-ac',       15, 3),
  ('sony-55-4k-google-tv',          12, 3),
  ('samsung-43-4k-tv',              18, 3),
  ('godrej-340l-double-door-fridge', 9, 3),
  ('lg-260l-fridge',                14, 3),
  ('samsung-7kg-front-load-wm',     11, 3),
  ('sony-51-home-theatre',          20, 3),
  ('prestige-3burner-gas-stove',    40, 5),
  ('prestige-mixer-grinder',        35, 5),
  ('sleepwell-queen-mattress',       8, 3),
  ('jl-3seater-sofa',                6, 3)
) as v(slug, qty, reorder)
join products p on p.slug = v.slug
on conflict (product_id) do nothing;

-- ── Ensure the optimistic-lock version is never NULL ──────────────────────
--  Hibernate needs a non-null @Version for UPDATE (edit/delete/stock) to work.
update categories  set version = 0 where version is null;
update brands      set version = 0 where version is null;
update products    set version = 0 where version is null;
update inventories set version = 0 where version is null;
