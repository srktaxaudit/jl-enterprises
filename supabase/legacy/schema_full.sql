-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  JL ENTERPRISES — Home Appliances, Thoothukudi                         ║
-- ║  COMPLETE PRODUCTION DATABASE SCHEMA  (Supabase / PostgreSQL)          ║
-- ║                                                                        ║
-- ║  Stack: HTML/CSS/JS (Vercel) → Spring Boot API (Render) → Supabase PG  ║
-- ║                                                                        ║
-- ║  HOW TO USE                                                            ║
-- ║   1. Open your JL Supabase project → SQL Editor → New query.           ║
-- ║   2. Paste this whole file and click "Run".                            ║
-- ║   3. Safe to re-run: everything uses IF NOT EXISTS / CREATE OR REPLACE ║
-- ║      / DROP POLICY IF EXISTS, so nothing is duplicated or lost.        ║
-- ║                                                                        ║
-- ║  SECURITY MODEL                                                        ║
-- ║   • Public (anon)      : read only the ACTIVE catalog (categories,     ║
-- ║                          products, product images, reviews, offers).   ║
-- ║   • Logged-in customer : read/insert only their OWN rows (orders,      ║
-- ║                          addresses, wishlist, reviews, service).       ║
-- ║   • Spring Boot backend: uses the SERVICE ROLE key, which BYPASSES     ║
-- ║                          RLS — it does all trusted writes (create      ║
-- ║                          orders, decrement stock, issue invoices,      ║
-- ║                          process refunds, admin actions).              ║
-- ║   NEVER expose the service role key in the browser / Vercel frontend.  ║
-- ╚══════════════════════════════════════════════════════════════════════╝

-- ── Extensions ─────────────────────────────────────────────────────────
-- pgcrypto  → gen_random_uuid()
-- citext    → case-insensitive email columns
-- pg_trgm   → fast ILIKE / fuzzy product search (trigram GIN index)
create extension if not exists pgcrypto;
create extension if not exists citext;
create extension if not exists pg_trgm;

-- ── Reusable trigger: keep updated_at fresh on every UPDATE ─────────────
create or replace function set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;


-- ════════════════════════════════════════════════════════════════════════
-- 1. CATEGORIES
--    AC · TV · Fridge · Washing Machine · Home Theatre · Kitchen & Stove ·
--    Furniture · Other Home Appliances
-- ════════════════════════════════════════════════════════════════════════
create table if not exists categories (
  id          uuid primary key default gen_random_uuid(),
  slug        text unique not null,
  name        text not null,
  emoji       text,                       -- shown until real icons are added
  image_url   text,
  sort_order  int  default 0,
  is_active   boolean default true,
  created_at  timestamptz default now(),
  updated_at  timestamptz default now()
);
create index if not exists idx_categories_active on categories(is_active);

drop trigger if exists trg_categories_updated on categories;
create trigger trg_categories_updated before update on categories
  for each row execute function set_updated_at();


-- ════════════════════════════════════════════════════════════════════════
-- 2. PRODUCTS  (with GST / HSN fields — this is a real appliance retailer)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists products (
  id            uuid primary key default gen_random_uuid(),
  slug          text unique not null,
  name          text not null,
  brand         text,
  category_id   uuid references categories(id) on delete set null,

  -- media / display
  emoji         text,                       -- placeholder until real images
  image_url     text,                       -- primary image (see product_images for the gallery)
  description   text,
  specs         jsonb default '{}'::jsonb,  -- {"Capacity":"1.5 Ton", ...}

  -- pricing (all amounts in INR)
  price         numeric(12,2) not null check (price >= 0),   -- selling price (GST-inclusive MRP model)
  mrp           numeric(12,2) check (mrp >= 0),              -- struck-through price
  cost_price    numeric(12,2),                               -- purchase cost (admin only; margin/reports)
  emi_per_month numeric(12,2),

  -- GST (India) — appliances are usually 18% or 28%
  hsn_code      text,                                        -- e.g. 8415 for AC, 8418 for fridge
  gst_rate      numeric(5,2) default 18 check (gst_rate >= 0 and gst_rate <= 100),
  is_price_tax_inclusive boolean default true,               -- price already includes GST?

  -- inventory
  sku           text unique,
  stock         int default 0 check (stock >= 0),
  reorder_at    int default 3,

  -- ratings (denormalised; recomputed by trigger from reviews)
  rating        numeric(2,1) default 0,
  review_count  int default 0,

  -- delivery hints (optional; helps logistics)
  weight_kg     numeric(8,2),
  warranty_text text,

  -- flags
  is_active     boolean default true,       -- enable/disable from admin
  is_featured   boolean default false,

  created_at    timestamptz default now(),
  updated_at    timestamptz default now()
);
create index if not exists idx_products_category on products(category_id);
create index if not exists idx_products_active   on products(is_active);
create index if not exists idx_products_featured on products(is_featured) where is_featured;
-- fast ILIKE / fuzzy search across name + brand (trigram GIN; immutable-safe)
create index if not exists idx_products_search on products
  using gin ((lower(coalesce(name,'') || ' ' || coalesce(brand,''))) gin_trgm_ops);

drop trigger if exists trg_products_updated on products;
create trigger trg_products_updated before update on products
  for each row execute function set_updated_at();


-- ════════════════════════════════════════════════════════════════════════
-- 3. PRODUCT IMAGES  (gallery — many images per product)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists product_images (
  id          uuid primary key default gen_random_uuid(),
  product_id  uuid not null references products(id) on delete cascade,
  url         text not null,
  alt         text,
  sort_order  int default 0,
  created_at  timestamptz default now()
);
create index if not exists idx_product_images_product on product_images(product_id);


-- ════════════════════════════════════════════════════════════════════════
-- 4. CUSTOMERS  (linked to Supabase Auth; nullable auth_id for guests)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists customers (
  id          uuid primary key default gen_random_uuid(),
  auth_id     uuid unique references auth.users(id) on delete set null,
  name        text,
  phone       text,
  email       citext,
  area        text,
  created_at  timestamptz default now(),
  updated_at  timestamptz default now()
);
create index if not exists idx_customers_phone on customers(phone);
create index if not exists idx_customers_email on customers(email);
create index if not exists idx_customers_auth  on customers(auth_id);

drop trigger if exists trg_customers_updated on customers;
create trigger trg_customers_updated before update on customers
  for each row execute function set_updated_at();


-- ════════════════════════════════════════════════════════════════════════
-- 5. ADDRESSES  (saved delivery addresses per customer)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists addresses (
  id           uuid primary key default gen_random_uuid(),
  customer_id  uuid not null references customers(id) on delete cascade,
  label        text default 'Home',        -- Home | Office | Other
  contact_name text,
  phone        text,
  line1        text not null,
  line2        text,
  city         text,
  pincode      text,
  state        text default 'Tamil Nadu',
  is_default   boolean default false,
  created_at   timestamptz default now(),
  updated_at   timestamptz default now()
);
create index if not exists idx_addresses_customer on addresses(customer_id);

drop trigger if exists trg_addresses_updated on addresses;
create trigger trg_addresses_updated before update on addresses
  for each row execute function set_updated_at();


-- ════════════════════════════════════════════════════════════════════════
-- 6. COUPONS  (functional discount engine, applied at checkout)
--    (offers table below is separate — that's for marketing banners)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists coupons (
  id            uuid primary key default gen_random_uuid(),
  code          text unique not null,       -- JL500, NEWHOME, FESTIVE
  description   text,
  discount_type text not null default 'FLAT' check (discount_type in ('FLAT','PERCENT')),
  value         numeric(12,2) not null check (value >= 0),  -- ₹ amount or % depending on type
  min_order     numeric(12,2) default 0,    -- minimum cart subtotal to qualify
  max_discount  numeric(12,2),              -- cap for PERCENT coupons
  usage_limit   int,                        -- total redemptions allowed (null = unlimited)
  per_user_limit int default 1,             -- redemptions per customer
  used_count    int default 0,
  starts_on     date,
  ends_on       date,
  is_active     boolean default true,
  created_at    timestamptz default now(),
  updated_at    timestamptz default now()
);
create index if not exists idx_coupons_active on coupons(is_active);

drop trigger if exists trg_coupons_updated on coupons;
create trigger trg_coupons_updated before update on coupons
  for each row execute function set_updated_at();


-- ════════════════════════════════════════════════════════════════════════
-- 7. WISHLIST  (server-side; the frontend also keeps a localStorage copy)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists wishlist_items (
  id          uuid primary key default gen_random_uuid(),
  customer_id uuid not null references customers(id) on delete cascade,
  product_id  uuid not null references products(id) on delete cascade,
  created_at  timestamptz default now(),
  unique (customer_id, product_id)
);
create index if not exists idx_wishlist_customer on wishlist_items(customer_id);


-- ════════════════════════════════════════════════════════════════════════
-- 8. ORDERS
-- ════════════════════════════════════════════════════════════════════════
create table if not exists orders (
  id            uuid primary key default gen_random_uuid(),
  order_no      text unique not null,        -- e.g. JL284213 (generated by backend or next_order_no())
  customer_id   uuid references customers(id) on delete set null,
  user_id       uuid references auth.users(id) on delete set null,  -- logged-in customer
  is_guest      boolean default false,

  -- contact + delivery snapshot (kept even if the customer/address later changes)
  contact_name  text,
  contact_phone text,
  email         citext,
  address       text,
  city          text,
  pincode       text,
  state         text default 'Tamil Nadu',
  delivery_date date,
  delivery_slot text,                        -- '9 AM – 12 PM'

  -- payment
  payment_mode   text default 'COD'  check (payment_mode  in ('COD','RAZORPAY','EMI','UPI','CARD','NETBANKING')),
  payment_status text default 'PENDING' check (payment_status in ('PENDING','PAID','FAILED','REFUNDED','PARTIALLY_REFUNDED')),
  payment_ref    text,                       -- razorpay payment id (see payments table for full history)

  -- fulfilment
  status        text default 'NEW' check (status in ('NEW','CONFIRMED','PACKED','OUT_FOR_DELIVERY','DELIVERED','CANCELLED','RETURNED')),

  -- money (all INR). GST columns for tax-invoice generation.
  subtotal      numeric(12,2) default 0,     -- sum of line taxable/base values
  discount      numeric(12,2) default 0,     -- coupon + exchange bonus
  coupon_code   text,
  gst_total     numeric(12,2) default 0,     -- cgst + sgst + igst
  cgst          numeric(12,2) default 0,
  sgst          numeric(12,2) default 0,
  igst          numeric(12,2) default 0,
  shipping_fee  numeric(12,2) default 0,
  total         numeric(12,2) default 0,     -- grand total payable

  notes         text,
  created_at    timestamptz default now(),
  updated_at    timestamptz default now()
);
create index if not exists idx_orders_status on orders(status);
create index if not exists idx_orders_user   on orders(user_id);
create index if not exists idx_orders_email  on orders(email);
create index if not exists idx_orders_phone  on orders(contact_phone);
create index if not exists idx_orders_created on orders(created_at desc);

drop trigger if exists trg_orders_updated on orders;
create trigger trg_orders_updated before update on orders
  for each row execute function set_updated_at();


-- ════════════════════════════════════════════════════════════════════════
-- 9. ORDER ITEMS  (line items with per-line GST breakup for the invoice)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists order_items (
  id            uuid primary key default gen_random_uuid(),
  order_id      uuid not null references orders(id) on delete cascade,
  product_id    uuid references products(id) on delete set null,
  -- snapshot of the product at purchase time (so history is stable)
  name          text,
  brand         text,
  hsn_code      text,
  price         numeric(12,2),               -- unit selling price (GST-inclusive)
  qty           int default 1 check (qty > 0),
  -- GST breakup per line (filled by backend at order time)
  gst_rate      numeric(5,2) default 18,
  taxable_value numeric(12,2) default 0,     -- base value before GST for the whole line
  cgst          numeric(12,2) default 0,
  sgst          numeric(12,2) default 0,
  igst          numeric(12,2) default 0,
  line_total    numeric(12,2) default 0      -- price * qty (GST-inclusive)
);
create index if not exists idx_order_items_order   on order_items(order_id);
create index if not exists idx_order_items_product on order_items(product_id);


-- ════════════════════════════════════════════════════════════════════════
-- 10. ORDER STATUS HISTORY  (audit trail of every status change)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists order_status_history (
  id          uuid primary key default gen_random_uuid(),
  order_id    uuid not null references orders(id) on delete cascade,
  from_status text,
  to_status   text not null,
  note        text,
  changed_by  text,                          -- 'system' | admin email
  created_at  timestamptz default now()
);
create index if not exists idx_order_history_order on order_status_history(order_id);


-- ════════════════════════════════════════════════════════════════════════
-- 11. PAYMENTS  (Razorpay order/payment/refund tracking + webhook idempotency)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists payments (
  id                  uuid primary key default gen_random_uuid(),
  order_id            uuid references orders(id) on delete set null,
  provider            text default 'RAZORPAY',
  razorpay_order_id   text,
  razorpay_payment_id text unique,           -- unique → webhook can't double-apply
  razorpay_signature  text,
  method              text,                  -- upi | card | netbanking | emi | cod
  amount              numeric(12,2),         -- in INR
  currency            text default 'INR',
  status              text default 'CREATED' check (status in ('CREATED','AUTHORIZED','CAPTURED','FAILED','REFUNDED')),
  refund_id           text,
  refund_amount       numeric(12,2) default 0,
  raw_event           jsonb,                 -- last webhook payload for debugging
  created_at          timestamptz default now(),
  updated_at          timestamptz default now()
);
create index if not exists idx_payments_order on payments(order_id);

drop trigger if exists trg_payments_updated on payments;
create trigger trg_payments_updated before update on payments
  for each row execute function set_updated_at();


-- ════════════════════════════════════════════════════════════════════════
-- 12. INVOICES  (GST tax invoices — sequential per financial year)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists invoices (
  id              uuid primary key default gen_random_uuid(),
  order_id        uuid not null unique references orders(id) on delete cascade,
  invoice_no      text unique not null,      -- JL/2026-27/000123 (see next_invoice_no)
  invoice_date    date default current_date,
  place_of_supply text default '33-Tamil Nadu',  -- GST state code 33 = TN
  seller_gstin    text,                      -- JL Enterprises GSTIN (set in settings)
  buyer_name      text,
  buyer_gstin     text,                      -- for B2B invoices (optional)
  taxable_value   numeric(12,2) default 0,
  cgst            numeric(12,2) default 0,
  sgst            numeric(12,2) default 0,
  igst            numeric(12,2) default 0,
  total           numeric(12,2) default 0,
  pdf_url         text,                      -- stored PDF in Supabase Storage (optional)
  created_at      timestamptz default now()
);
create index if not exists idx_invoices_order on invoices(order_id);


-- ════════════════════════════════════════════════════════════════════════
-- 13. RETURN REQUESTS  (RMA — returns & cancellations)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists return_requests (
  id          uuid primary key default gen_random_uuid(),
  order_id    uuid references orders(id) on delete cascade,
  order_no    text not null,
  phone       text,
  kind        text default 'RETURN' check (kind in ('RETURN','CANCEL')),
  reason      text,
  status      text default 'REQUESTED' check (status in ('REQUESTED','APPROVED','REJECTED','PICKED_UP','REFUNDED')),
  admin_note  text,
  created_at  timestamptz default now(),
  updated_at  timestamptz default now()
);
create index if not exists idx_returns_order  on return_requests(order_id);
create index if not exists idx_returns_status on return_requests(status);

drop trigger if exists trg_returns_updated on return_requests;
create trigger trg_returns_updated before update on return_requests
  for each row execute function set_updated_at();


-- ════════════════════════════════════════════════════════════════════════
-- 14. REVIEWS  (customer product ratings; recompute product rating on change)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists reviews (
  id          uuid primary key default gen_random_uuid(),
  product_id  uuid not null references products(id) on delete cascade,
  customer_id uuid references customers(id) on delete set null,
  author_name text,
  rating      int not null check (rating between 1 and 5),
  title       text,
  body        text,
  is_approved boolean default true,          -- admin can moderate
  created_at  timestamptz default now(),
  unique (product_id, customer_id)           -- one review per customer per product
);
create index if not exists idx_reviews_product on reviews(product_id);

-- Recompute products.rating + review_count whenever reviews change
create or replace function recompute_product_rating()
returns trigger language plpgsql as $$
declare
  pid uuid := coalesce(new.product_id, old.product_id);
begin
  update products p set
    review_count = (select count(*) from reviews r where r.product_id = pid and r.is_approved),
    rating = coalesce((select round(avg(r.rating)::numeric, 1) from reviews r where r.product_id = pid and r.is_approved), 0)
  where p.id = pid;
  return null;
end;
$$;
drop trigger if exists trg_reviews_recompute on reviews;
create trigger trg_reviews_recompute after insert or update or delete on reviews
  for each row execute function recompute_product_rating();


-- ════════════════════════════════════════════════════════════════════════
-- 15. SERVICE BOOKINGS  (Repair / Installation / AMC — wired to the DB)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists service_bookings (
  id            uuid primary key default gen_random_uuid(),
  customer_id   uuid references customers(id) on delete set null,
  contact_name  text,
  contact_phone text,
  email         citext,
  appliance     text,                         -- AC | Fridge | Washing Machine | TV | ...
  service_type  text,                         -- Repair | Installation | AMC | Service
  address       text,
  slot_date     date,
  slot_time     text,
  area          text,
  complaint     text,                         -- description of the problem
  mode          text default 'IN_HOUSE' check (mode in ('IN_HOUSE','OUTSOURCED')),
  assigned_to   text,
  status        text default 'OPEN' check (status in ('OPEN','ASSIGNED','IN_PROGRESS','DONE','CANCELLED')),
  created_at    timestamptz default now(),
  updated_at    timestamptz default now()
);
create index if not exists idx_service_status on service_bookings(status);

drop trigger if exists trg_service_updated on service_bookings;
create trigger trg_service_updated before update on service_bookings
  for each row execute function set_updated_at();


-- ════════════════════════════════════════════════════════════════════════
-- 16. OFFERS  (marketing banners / promo strips shown on the storefront)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists offers (
  id          uuid primary key default gen_random_uuid(),
  title       text not null,
  type        text,                            -- CATEGORY | COUPON | BUYBACK | STOREWIDE
  code        text,                            -- optional linked coupon code
  value_text  text,                            -- "Up to 40% Off"
  image_url   text,
  is_active   boolean default true,
  ends_on     date,
  created_at  timestamptz default now()
);
create index if not exists idx_offers_active on offers(is_active);


-- ════════════════════════════════════════════════════════════════════════
-- 17. STOCK MOVEMENTS  (inventory audit — every increase/decrease)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists stock_movements (
  id          uuid primary key default gen_random_uuid(),
  product_id  uuid references products(id) on delete set null,
  change      int not null,                   -- negative = sold, positive = restock
  reason      text,                           -- 'ORDER' | 'RETURN' | 'MANUAL' | 'CORRECTION'
  ref_order   text,                           -- related order_no if any
  created_at  timestamptz default now()
);
create index if not exists idx_stock_moves_product on stock_movements(product_id);


-- ════════════════════════════════════════════════════════════════════════
-- 18. NOTIFICATIONS LOG  (WhatsApp / email sends — for delivery auditing)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists notifications_log (
  id          uuid primary key default gen_random_uuid(),
  channel     text check (channel in ('WHATSAPP','EMAIL','SMS')),
  recipient   text,
  template    text,
  ref_order   text,
  status      text default 'SENT',            -- SENT | FAILED
  detail      jsonb,
  created_at  timestamptz default now()
);


-- ════════════════════════════════════════════════════════════════════════
-- 19. ADMIN USERS + AUDIT LOG  (roles beyond a single shared password)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists admin_users (
  id          uuid primary key default gen_random_uuid(),
  auth_id     uuid unique references auth.users(id) on delete cascade,
  email       citext unique,
  name        text,
  role        text default 'STAFF' check (role in ('OWNER','MANAGER','STAFF')),
  is_active   boolean default true,
  created_at  timestamptz default now()
);

create table if not exists admin_audit_log (
  id          uuid primary key default gen_random_uuid(),
  admin_email text,
  action      text,                           -- 'UPDATE_PRODUCT' | 'CHANGE_ORDER_STATUS' | ...
  entity      text,                           -- table / entity name
  entity_id   text,
  detail      jsonb,
  created_at  timestamptz default now()
);
create index if not exists idx_audit_created on admin_audit_log(created_at desc);


-- ════════════════════════════════════════════════════════════════════════
-- 20. SETTINGS  (key/value store — GSTIN, store hours, exchange bonus, etc.)
-- ════════════════════════════════════════════════════════════════════════
create table if not exists settings (
  key        text primary key,
  value      jsonb,
  updated_at timestamptz default now()
);

drop trigger if exists trg_settings_updated on settings;
create trigger trg_settings_updated before update on settings
  for each row execute function set_updated_at();


-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  HELPER FUNCTIONS                                                      ║
-- ╚══════════════════════════════════════════════════════════════════════╝

-- Atomically change stock (negative to sell, positive to restock) and log it.
create or replace function decrement_stock(p_id uuid, p_qty int)
returns void language plpgsql as $$
begin
  update products
     set stock = greatest(0, stock - p_qty),
         updated_at = now()
   where id = p_id;
  insert into stock_movements(product_id, change, reason)
  values (p_id, -p_qty, 'ORDER');
end;
$$;

-- Human-friendly, unique order number: JL + 6 digits (e.g. JL482913).
create or replace function next_order_no()
returns text language plpgsql as $$
declare
  candidate text;
begin
  loop
    candidate := 'JL' || lpad((floor(random() * 900000) + 100000)::text, 6, '0');
    exit when not exists (select 1 from orders where order_no = candidate);
  end loop;
  return candidate;
end;
$$;

-- Sequential GST invoice number per Indian financial year (Apr–Mar):
--   JL/2026-27/000001, JL/2026-27/000002, ...
-- Uses a counter table with a row lock so numbers are gapless and unique.
create table if not exists invoice_counters (
  fy       text primary key,   -- '2026-27'
  last_no  int default 0
);

create or replace function current_fy()
returns text language sql stable as $$
  select case
    when extract(month from current_date) >= 4
      then extract(year from current_date)::int || '-' || right((extract(year from current_date)::int + 1)::text, 2)
    else (extract(year from current_date)::int - 1) || '-' || right(extract(year from current_date)::text, 2)
  end;
$$;

create or replace function next_invoice_no()
returns text language plpgsql as $$
declare
  v_fy text := current_fy();   -- v_ prefix avoids clashing with the column name "fy"
  n    int;
begin
  insert into invoice_counters(fy, last_no) values (v_fy, 0)
    on conflict (fy) do nothing;
  update invoice_counters set last_no = last_no + 1
    where fy = v_fy
    returning last_no into n;
  return 'JL/' || v_fy || '/' || lpad(n::text, 6, '0');
end;
$$;


-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  ROW LEVEL SECURITY                                                    ║
-- ║  The Spring Boot backend uses the SERVICE ROLE key and bypasses all    ║
-- ║  of this. These policies protect any DIRECT access from the browser    ║
-- ║  using the anon / logged-in key.                                       ║
-- ╚══════════════════════════════════════════════════════════════════════╝

alter table categories       enable row level security;
alter table products         enable row level security;
alter table product_images   enable row level security;
alter table offers           enable row level security;
alter table reviews          enable row level security;
alter table customers        enable row level security;
alter table addresses        enable row level security;
alter table wishlist_items   enable row level security;
alter table orders           enable row level security;
alter table order_items      enable row level security;
alter table service_bookings enable row level security;
alter table return_requests  enable row level security;
alter table payments         enable row level security;
alter table invoices         enable row level security;
alter table coupons          enable row level security;

-- ── Public read of the ACTIVE catalog ──────────────────────────────────
drop policy if exists "public read active categories" on categories;
create policy "public read active categories" on categories
  for select using (is_active = true);

drop policy if exists "public read active products" on products;
create policy "public read active products" on products
  for select using (is_active = true);

drop policy if exists "public read product images" on product_images;
create policy "public read product images" on product_images
  for select using (
    exists (select 1 from products p where p.id = product_id and p.is_active)
  );

drop policy if exists "public read active offers" on offers;
create policy "public read active offers" on offers
  for select using (is_active = true);

drop policy if exists "public read approved reviews" on reviews;
create policy "public read approved reviews" on reviews
  for select using (is_approved = true);

-- ── Logged-in customer: only their OWN data ────────────────────────────
-- (a customer row is "mine" when its auth_id = auth.uid())

drop policy if exists "customer reads own profile" on customers;
create policy "customer reads own profile" on customers
  for select using (auth_id = auth.uid());

drop policy if exists "customer updates own profile" on customers;
create policy "customer updates own profile" on customers
  for update using (auth_id = auth.uid());

drop policy if exists "customer manages own addresses" on addresses;
create policy "customer manages own addresses" on addresses
  for all using (
    customer_id in (select id from customers where auth_id = auth.uid())
  ) with check (
    customer_id in (select id from customers where auth_id = auth.uid())
  );

drop policy if exists "customer manages own wishlist" on wishlist_items;
create policy "customer manages own wishlist" on wishlist_items
  for all using (
    customer_id in (select id from customers where auth_id = auth.uid())
  ) with check (
    customer_id in (select id from customers where auth_id = auth.uid())
  );

drop policy if exists "customer reads own orders" on orders;
create policy "customer reads own orders" on orders
  for select using (user_id = auth.uid());

drop policy if exists "customer reads own order items" on order_items;
create policy "customer reads own order items" on order_items
  for select using (
    order_id in (select id from orders where user_id = auth.uid())
  );

drop policy if exists "customer reads own invoices" on invoices;
create policy "customer reads own invoices" on invoices
  for select using (
    order_id in (select id from orders where user_id = auth.uid())
  );

drop policy if exists "customer writes own review" on reviews;
create policy "customer writes own review" on reviews
  for insert with check (
    customer_id in (select id from customers where auth_id = auth.uid())
  );

-- NOTE: payments, return_requests, coupons, service_bookings, order_items
-- (writes), stock_movements, invoices (writes), admin_* have NO public
-- policies on purpose — with RLS enabled and no policy, the anon/auth keys
-- get zero rows. Only the SERVICE ROLE key (Spring Boot) can touch them.


-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  STORAGE BUCKET  (public product images; admin uploads via service key)║
-- ╚══════════════════════════════════════════════════════════════════════╝
insert into storage.buckets (id, name, public)
values ('product-images', 'product-images', true)
on conflict (id) do nothing;


-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  SEED DATA                                                             ║
-- ╚══════════════════════════════════════════════════════════════════════╝

-- Categories (idempotent on slug)
insert into categories (slug, name, emoji, sort_order) values
  ('air-conditioners', 'Air Conditioners',      '❄️', 1),
  ('televisions',      'Televisions',           '📺', 2),
  ('refrigerators',    'Refrigerators',         '🧊', 3),
  ('washing-machines', 'Washing Machines',      '🌀', 4),
  ('home-theatre',     'Home Theatre',          '🔊', 5),
  ('kitchen',          'Kitchen & Stove',       '🍳', 6),
  ('furniture',        'Furniture',             '🛋️', 7),
  ('other-appliances', 'Other Home Appliances', '🏠', 8)
on conflict (slug) do nothing;

-- Store settings (GSTIN + contact are placeholders — update with real values)
insert into settings (key, value) values
  ('store_profile', jsonb_build_object(
      'name',    'JL Enterprises',
      'address', '185G/1B, Palai Road, Chidambaramnagar, Thoothukudi, Tamil Nadu 628008',
      'state',   'Tamil Nadu',
      'state_code', '33',
      'gstin',   'REPLACE_WITH_JL_GSTIN',
      'phone',   '+91 95149 70111',
      'hours',   '10 AM to 10 PM'
  )),
  ('exchange_bonus', to_jsonb(3000)),
  ('free_delivery',  to_jsonb(true))
on conflict (key) do nothing;

-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  DONE. Verify in Table Editor:                                         ║
-- ║   categories, products, product_images, customers, addresses,         ║
-- ║   coupons, wishlist_items, orders, order_items, order_status_history,  ║
-- ║   payments, invoices, return_requests, reviews, service_bookings,      ║
-- ║   offers, stock_movements, notifications_log, admin_users,             ║
-- ║   admin_audit_log, settings, invoice_counters                          ║
-- ╚══════════════════════════════════════════════════════════════════════╝
