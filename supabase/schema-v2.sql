-- ════════════════════════════════════════════════════════════════════
--  JL ENTERPRISES — Schema v2 (additive migration)
--  Run AFTER schema.sql in the JL Supabase project. Safe to re-run.
--  Adds: delivery slots, order email + auth linkage, Razorpay payment
--  reference, and the returns/refunds (RMA) table.
-- ════════════════════════════════════════════════════════════════════

-- ── Orders: delivery slot, email, auth user, payment reference ──────
alter table orders add column if not exists email          text;
alter table orders add column if not exists user_id        uuid;   -- auth.users id (nullable for guests)
alter table orders add column if not exists delivery_date  date;
alter table orders add column if not exists delivery_slot  text;   -- e.g. '9 AM – 12 PM'
alter table orders add column if not exists payment_ref    text;   -- razorpay payment id

create index if not exists idx_orders_user  on orders(user_id);
create index if not exists idx_orders_email on orders(email);
create index if not exists idx_orders_phone on orders(contact_phone);

-- ── Return / cancellation requests (RMA) ────────────────────────────
create table if not exists return_requests (
  id          uuid primary key default gen_random_uuid(),
  order_id    uuid references orders(id) on delete cascade,
  order_no    text not null,
  phone       text,
  kind        text default 'RETURN',     -- RETURN | CANCEL
  reason      text,
  status      text default 'REQUESTED',  -- REQUESTED | APPROVED | REJECTED | PICKED_UP | REFUNDED
  admin_note  text,
  created_at  timestamptz default now(),
  updated_at  timestamptz default now()
);
create index if not exists idx_returns_order  on return_requests(order_id);
create index if not exists idx_returns_status on return_requests(status);

-- return_requests: NO public policies — reachable only via the
-- service-role key on the server, same as orders.
alter table return_requests enable row level security;
