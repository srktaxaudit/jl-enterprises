-- ════════════════════════════════════════════════════════════════════════
--  JL E-COMMERCE BACKEND — schema for Supabase (matches the JPA entities in
--  ecommerce-backend). Paste this whole file into Supabase → SQL Editor → Run.
--
--  This is the SAME schema Hibernate would create on first boot. After running
--  it, deploy the backend with JPA_DDL_AUTO=update (forgiving: it only adds
--  anything missing, never drops). Safe to re-run (IF NOT EXISTS everywhere).
--
--  NOTE: this is the Java backend's schema — do NOT also run schema_full.sql
--  (that belongs to the old Next.js store and its table names would clash).
-- ════════════════════════════════════════════════════════════════════════

-- ── Identity & auth ─────────────────────────────────────────────────────
create table if not exists roles (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  name varchar(40), description varchar(200),
  constraint uk_role_name unique (name)
);

create table if not exists permissions (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  name varchar(80), description varchar(200),
  constraint uk_permission_name unique (name)
);

create table if not exists users (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  email varchar(160), phone varchar(20), password_hash varchar(100),
  first_name varchar(80), last_name varchar(80),
  email_verified boolean not null default false, phone_verified boolean not null default false,
  enabled boolean not null default true, account_locked boolean not null default false,
  failed_login_attempts int not null default 0, locked_until timestamptz, last_login_at timestamptz,
  provider varchar(20) not null default 'LOCAL', provider_id varchar(120),
  constraint uk_user_email unique (email), constraint uk_user_phone unique (phone)
);

create table if not exists role_permissions (
  role_id uuid not null references roles(id) on delete cascade,
  permission_id uuid not null references permissions(id) on delete cascade,
  primary key (role_id, permission_id)
);

create table if not exists user_roles (
  user_id uuid not null references users(id) on delete cascade,
  role_id uuid not null references roles(id) on delete cascade,
  primary key (user_id, role_id)
);

create table if not exists refresh_tokens (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  token varchar(200), user_id uuid references users(id) on delete cascade,
  expires_at timestamptz, revoked boolean not null default false, remember_me boolean not null default false,
  user_agent varchar(255), ip_address varchar(60),
  constraint uk_refresh_token unique (token)
);

create table if not exists otps (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  identifier varchar(160), code_hash varchar(100), purpose varchar(30),
  expires_at timestamptz, attempts int not null default 0, consumed boolean not null default false
);

create table if not exists addresses (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  user_id uuid not null references users(id),
  type varchar(20) not null default 'SHIPPING', full_name varchar(120), phone varchar(20),
  line1 varchar(200), line2 varchar(200), city varchar(80), state varchar(80),
  postal_code varchar(20), country varchar(80) default 'India', is_default boolean not null default false
);

-- ── Catalog ─────────────────────────────────────────────────────────────
create table if not exists categories (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  name varchar(120), slug varchar(140), description text, image_url varchar(500),
  sort_order int not null default 0, parent_id uuid references categories(id),
  constraint uk_category_slug unique (slug)
);

create table if not exists brands (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  name varchar(120), slug varchar(140), logo_url varchar(500), description text,
  constraint uk_brand_slug unique (slug)
);

create table if not exists products (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  name varchar(200), slug varchar(220), sku varchar(80),
  short_description varchar(500), description text,
  category_id uuid references categories(id), brand_id uuid references brands(id),
  price numeric(12,2), compare_price numeric(12,2), discount_percent numeric(5,2),
  currency varchar(3) not null default 'INR', featured boolean not null default false,
  meta_title varchar(200), meta_description varchar(300),
  average_rating numeric(3,2), review_count int not null default 0,
  view_count bigint not null default 0, sales_count bigint not null default 0,
  constraint uk_product_slug unique (slug), constraint uk_product_sku unique (sku)
);

create table if not exists product_images (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  product_id uuid not null references products(id),
  url varchar(500), alt_text varchar(200), sort_order int not null default 0, is_primary boolean not null default false
);

create table if not exists product_variants (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  product_id uuid not null references products(id),
  sku varchar(80), title varchar(160), price numeric(12,2), compare_price numeric(12,2),
  constraint uk_variant_sku unique (sku)
);

create table if not exists variant_options (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  variant_id uuid not null references product_variants(id),
  name varchar(60), value varchar(120)
);

create table if not exists inventories (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  product_id uuid not null references products(id),
  quantity int not null default 0, reserved int not null default 0,
  reorder_level int not null default 3, warehouse_location varchar(120),
  constraint uk_inventory_product unique (product_id)
);

-- ── Cart & wishlist ───────────────────────────────────────────────────────
create table if not exists carts (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  user_id uuid not null references users(id),
  constraint uk_cart_user unique (user_id)
);

create table if not exists cart_items (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  cart_id uuid not null references carts(id), product_id uuid not null references products(id),
  variant_id uuid references product_variants(id), quantity int not null default 1, unit_price numeric(12,2)
);

create table if not exists wishlists (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  user_id uuid not null references users(id),
  constraint uk_wishlist_user unique (user_id)
);

create table if not exists wishlist_items (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  wishlist_id uuid not null references wishlists(id), product_id uuid not null references products(id),
  constraint uk_wishlist_product unique (wishlist_id, product_id)
);

-- ── Marketing ─────────────────────────────────────────────────────────────
create table if not exists coupons (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  code varchar(40), description varchar(200), type varchar(20), value numeric(12,2),
  min_order_amount numeric(12,2), max_discount numeric(12,2),
  usage_limit int, used_count int not null default 0, per_user_limit int,
  starts_at timestamptz, expires_at timestamptz, active boolean not null default true,
  constraint uk_coupon_code unique (code)
);

-- ── Orders & payments ─────────────────────────────────────────────────────
create table if not exists orders (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  order_number varchar(30), user_id uuid not null references users(id),
  order_status varchar(20),
  subtotal numeric(12,2) not null default 0, discount_total numeric(12,2) not null default 0,
  tax_total numeric(12,2) not null default 0, shipping_total numeric(12,2) not null default 0,
  grand_total numeric(12,2) not null default 0, currency varchar(3) not null default 'INR',
  coupon_id uuid references coupons(id),
  ship_full_name varchar(120), ship_phone varchar(20), ship_line1 varchar(200), ship_line2 varchar(200),
  ship_city varchar(80), ship_state varchar(80), ship_postal_code varchar(20), ship_country varchar(80),
  bill_full_name varchar(120), bill_phone varchar(20), bill_line1 varchar(200), bill_line2 varchar(200),
  bill_city varchar(80), bill_state varchar(80), bill_postal_code varchar(20), bill_country varchar(80),
  placed_at timestamptz, notes varchar(500),
  constraint uk_order_number unique (order_number)
);

create table if not exists order_items (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  order_id uuid not null references orders(id), product_id uuid references products(id),
  variant_id uuid references product_variants(id), product_name varchar(200), sku varchar(80),
  unit_price numeric(12,2), quantity int, line_total numeric(12,2)
);

create table if not exists payments (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  order_id uuid not null references orders(id),
  method varchar(20), payment_status varchar(20), amount numeric(12,2), currency varchar(3) not null default 'INR',
  provider varchar(40), provider_payment_id varchar(120),
  constraint uk_payment_order unique (order_id)
);

create table if not exists transactions (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  payment_id uuid not null references payments(id),
  type varchar(20), txn_status varchar(20), amount numeric(12,2),
  provider_reference varchar(160), raw_response text, processed_at timestamptz
);

create table if not exists coupon_usages (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  coupon_id uuid not null references coupons(id), user_id uuid not null references users(id),
  order_id uuid references orders(id), discount_applied numeric(12,2)
);

-- ── Reviews, notifications, banners, audit, settings ──────────────────────
create table if not exists reviews (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  product_id uuid not null references products(id), user_id uuid not null references users(id),
  rating int, title varchar(160), comment text,
  review_status varchar(20) not null default 'PENDING', verified_purchase boolean not null default false,
  constraint uk_review_user_product unique (user_id, product_id)
);

create table if not exists notifications (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  user_id uuid not null references users(id), type varchar(20) not null default 'SYSTEM',
  title varchar(160), message text, link varchar(500), is_read boolean not null default false, read_at timestamptz
);

create table if not exists banners (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  title varchar(160), image_url varchar(500), link_url varchar(500), position varchar(40),
  sort_order int not null default 0, starts_at timestamptz, ends_at timestamptz, active boolean not null default true
);

create table if not exists audit_logs (
  id uuid primary key, created_at timestamptz, updated_at timestamptz,
  created_by varchar(120), updated_by varchar(120),
  status varchar(20) not null default 'ACTIVE', deleted boolean not null default false, version bigint,
  actor varchar(160), action varchar(80), entity varchar(80), entity_id varchar(80),
  detail text, ip_address varchar(60), user_agent varchar(255)
);

create table if not exists app_settings (
  setting_key varchar(120) primary key, setting_value text, updated_at timestamptz
);

-- Helpful indexes
create index if not exists idx_product_category on products(category_id);
create index if not exists idx_order_user on orders(user_id);
create index if not exists idx_order_status on orders(order_status);
create index if not exists idx_notification_user on notifications(user_id);
create index if not exists idx_audit_created on audit_logs(created_at desc);
