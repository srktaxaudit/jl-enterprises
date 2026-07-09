-- ════════════════════════════════════════════════════════════════════
--  JL ENTERPRISES — Store schema (Supabase / Postgres)
--  Run this in the JL Supabase project (SQL editor) — NOT the SRK DB.
-- ════════════════════════════════════════════════════════════════════

-- ── Categories ──────────────────────────────────────────────────────
create table if not exists categories (
  id          uuid primary key default gen_random_uuid(),
  slug        text unique not null,
  name        text not null,
  emoji       text,
  sort_order  int default 0,
  is_active   boolean default true,
  created_at  timestamptz default now()
);

-- ── Products ────────────────────────────────────────────────────────
create table if not exists products (
  id            uuid primary key default gen_random_uuid(),
  slug          text unique not null,
  name          text not null,
  brand         text,
  category_id   uuid references categories(id) on delete set null,
  emoji         text,                       -- placeholder until real images
  image_url     text,
  description   text,
  specs         jsonb default '{}'::jsonb,
  price         numeric(12,2) not null,     -- selling price
  mrp           numeric(12,2),              -- struck-through price
  stock         int default 0,
  reorder_at    int default 3,
  rating        numeric(2,1) default 4.5,
  review_count  int default 0,
  emi_per_month numeric(12,2),
  is_active     boolean default true,       -- enable/disable from admin
  is_featured   boolean default false,
  created_at    timestamptz default now(),
  updated_at    timestamptz default now()
);
create index if not exists idx_products_category on products(category_id);
create index if not exists idx_products_active   on products(is_active);

-- ── Customers ───────────────────────────────────────────────────────
create table if not exists customers (
  id          uuid primary key default gen_random_uuid(),
  auth_id     uuid,                          -- links to auth.users (nullable for guests)
  name        text,
  phone       text,
  email       text,
  area        text,
  created_at  timestamptz default now()
);
create index if not exists idx_customers_phone on customers(phone);

-- ── Orders ──────────────────────────────────────────────────────────
create table if not exists orders (
  id            uuid primary key default gen_random_uuid(),
  order_no      text unique not null,        -- e.g. JL2842
  customer_id   uuid references customers(id) on delete set null,
  is_guest      boolean default false,
  contact_name  text,
  contact_phone text,
  address       text,
  city          text,
  pincode       text,
  state         text default 'Tamil Nadu',
  payment_mode  text default 'COD',          -- COD | RAZORPAY | EMI
  payment_status text default 'PENDING',     -- PENDING | PAID
  status        text default 'NEW',          -- NEW | PACKED | OUT_FOR_DELIVERY | DELIVERED | CANCELLED
  subtotal      numeric(12,2) default 0,
  discount      numeric(12,2) default 0,
  total         numeric(12,2) default 0,
  created_at    timestamptz default now()
);
create index if not exists idx_orders_status on orders(status);

-- ── Order items ─────────────────────────────────────────────────────
create table if not exists order_items (
  id          uuid primary key default gen_random_uuid(),
  order_id    uuid references orders(id) on delete cascade,
  product_id  uuid references products(id) on delete set null,
  name        text,
  brand       text,
  price       numeric(12,2),
  qty         int default 1
);

-- ── Service bookings ────────────────────────────────────────────────
create table if not exists service_bookings (
  id            uuid primary key default gen_random_uuid(),
  customer_id   uuid references customers(id) on delete set null,
  contact_name  text,
  contact_phone text,
  appliance     text,                         -- AC | Fridge | Washing Machine | TV
  service_type  text,                         -- Repair | Installation | AMC | Service
  slot_date     date,
  slot_time     text,
  area          text,
  mode          text default 'IN_HOUSE',      -- IN_HOUSE | OUTSOURCED
  assigned_to   text,
  status        text default 'OPEN',          -- OPEN | ASSIGNED | DONE
  created_at    timestamptz default now()
);

-- ── Offers ──────────────────────────────────────────────────────────
create table if not exists offers (
  id          uuid primary key default gen_random_uuid(),
  title       text not null,
  type        text,                            -- CATEGORY | COUPON | BUYBACK | STOREWIDE
  code        text,
  value_text  text,
  is_active   boolean default true,
  ends_on     date,
  created_at  timestamptz default now()
);

-- ── Settings (key/value) ────────────────────────────────────────────
create table if not exists settings (
  key   text primary key,
  value jsonb
);

-- ════════════════════════════════════════════════════════════════════
--  Row Level Security
--  Public can READ active catalog; writes go through server (service key).
-- ════════════════════════════════════════════════════════════════════
alter table products   enable row level security;
alter table categories enable row level security;
alter table offers     enable row level security;

drop policy if exists "public read active products" on products;
create policy "public read active products" on products
  for select using (is_active = true);

drop policy if exists "public read active categories" on categories;
create policy "public read active categories" on categories
  for select using (is_active = true);

drop policy if exists "public read active offers" on offers;
create policy "public read active offers" on offers
  for select using (is_active = true);

-- orders / customers / service_bookings: no public policies →
-- only reachable via the service-role key on the server (secure by default).

-- ════════════════════════════════════════════════════════════════════
--  Helper: atomically reduce stock when an order is placed.
-- ════════════════════════════════════════════════════════════════════
create or replace function decrement_stock(p_id uuid, p_qty int)
returns void language sql as $$
  update products
     set stock = greatest(0, stock - p_qty),
         updated_at = now()
   where id = p_id;
$$;

-- ════════════════════════════════════════════════════════════════════
--  Storage bucket for product images (admin uploads via service role).
--  Public read so the storefront can show images.
-- ════════════════════════════════════════════════════════════════════
insert into storage.buckets (id, name, public)
values ('product-images', 'product-images', true)
on conflict (id) do nothing;
