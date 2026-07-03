-- ════════════════════════════════════════════════════════════════════
--  JL ENTERPRISES — Schema v3 (admin authentication)
--  Run AFTER schema_full.sql in the JL Supabase project. Safe to re-run.
--
--  Purpose: make the admin panel usable with a real, per-user login handled
--  by the Spring Boot API (not a single shared password). Passwords are
--  BCrypt-hashed. The API verifies them; the browser never sees a hash.
--
--  NOTE: admin_users.auth_id (from schema_full.sql) links to Supabase Auth
--  and stays NULLABLE — this self-contained admin login does not require a
--  matching auth.users row. If you later switch to Supabase Auth as the
--  identity provider, populate auth_id and stop using password_hash.
-- ════════════════════════════════════════════════════════════════════

create extension if not exists pgcrypto;   -- gen_salt('bf') / crypt() for BCrypt

-- ── Admin login columns ─────────────────────────────────────────────
alter table admin_users add column if not exists password_hash text;
alter table admin_users add column if not exists last_login_at  timestamptz;

-- ── Seed the OWNER (CEO) account ────────────────────────────────────
--  Temporary password below — CHANGE IT immediately after first login via
--  POST /api/admin/auth/change-password. gen_salt('bf') produces a $2a$
--  BCrypt hash that Spring Security's BCryptPasswordEncoder can verify.
insert into admin_users (email, name, role, is_active, password_hash)
values (
  'owner@jlenterprises.in',
  'JL Owner',
  'OWNER',
  true,
  crypt('ChangeMe@123', gen_salt('bf'))
)
on conflict (email) do nothing;

-- ── (Optional) example team members — uncomment and set real emails ──
-- insert into admin_users (email, name, role, is_active, password_hash) values
--   ('manager@jlenterprises.in', 'Store Manager', 'MANAGER', true, crypt('ChangeMe@123', gen_salt('bf'))),
--   ('sales@jlenterprises.in',   'Sales Staff',   'STAFF',   true, crypt('ChangeMe@123', gen_salt('bf')))
-- on conflict (email) do nothing;

-- ── Helper index for fast, case-insensitive email lookup on login ───
create index if not exists idx_admin_users_email on admin_users(email);
