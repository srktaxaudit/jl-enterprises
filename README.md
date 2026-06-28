# JL Enterprises — Online Store

E-commerce store for **JL Enterprises** (Home Appliances & Furniture, Thoothukudi).
Built with **Next.js 14 (App Router) + TypeScript + Tailwind + Supabase**.

## Status

**Phase 1 (Storefront MVP) ✅**
- Home, category, product detail, cart, checkout, service booking, login
- Floating cart, live cart state (localStorage), responsive design

**Phase 2 (Supabase wiring + auth) ✅ — graceful seed fallback**
- Server data layer (`lib/data.ts`) reads from Supabase when keys are set, else seed
- Email-OTP auth via Supabase Auth (`/login`) + guest checkout; session middleware
- Order persistence: `POST /api/orders` inserts order + items via service role, decrements stock
- `supabase/schema.sql` + `supabase/seed.sql` ready to run

**Phase 3a (Razorpay + WhatsApp) ✅ — demo fallback**
- Razorpay: `POST /api/razorpay/order` (create) + `/api/razorpay/verify` (HMAC signature check) → checkout opens the Razorpay modal; falls back to demo order without keys
- Shared `lib/orders.ts` persists order (PAID for Razorpay, PENDING for COD) + decrements stock
- WhatsApp (`lib/whatsapp.ts`, Meta Cloud API): order confirmation on every order; `POST /api/whatsapp/broadcast` (token-guarded) for bulk offers — no-op without creds

**Phase 3b (Admin / CEO control centre) ✅**
- `/admin` panel, password-gated (middleware) — demo password `jladmin` (set `ADMIN_PASSWORD`)
- Dashboard (live stats), Orders (status update → WhatsApp), Products (enable/disable),
  Inventory (stock edit), Offers, Customers, WhatsApp broadcast UI, Teams & Settings (roles)
- Store + admin split via route groups: `app/(store)` and `app/admin/(panel)`
- All actions persist via service-role client when Supabase is set, else demo no-op

**Phase 4 (Mobile app + product CRUD + uploads) ✅**
- Product **create/edit** forms (`/admin/products/new`, `/admin/products/[id]/edit`) + `POST`/`PATCH /api/admin/products`
- **Image upload** to Supabase Storage (`POST /api/admin/upload`, bucket `product-images`) — storefront shows uploaded image, else emoji
- **PWA**: `app/manifest.ts` + `public/icon.svg` + `public/sw.js` + meta → installable (Add to Home Screen) for customers and staff
- **Capacitor**: `capacitor.config.ts` (wraps the live site) + `MOBILE.md` build guide for native customer & staff apps

**Still pending**
- ⏳ Phone-OTP (needs SMS provider) · per-team granular role enforcement · native APK/IPA builds (need Android Studio/Xcode — see MOBILE.md)

## Admin access
Visit `/admin` → login with `jladmin` (or your `ADMIN_PASSWORD`).

> Until JL's Supabase keys are in `.env.local`, the store runs on seed data and
> auth/checkout work in safe demo mode — no errors, nothing to break.

## Run locally

```bash
npm install
npm run dev
# open http://localhost:3000
```

The store works immediately with sample products (`lib/catalog.ts`).

## Connect Supabase (Phase 2)

1. Create a **new, separate** Supabase project for JL (not the SRK database).
2. Run `supabase/schema.sql` in the Supabase SQL editor.
3. Copy `.env.example` → `.env.local` and fill in JL's keys.
4. Swap the seed getters in `lib/catalog.ts` for Supabase queries.

## Keep separate from SRK (important)

| Service | Rule |
|---|---|
| Supabase | New isolated project for JL |
| Razorpay | JL's own account → settles to JL's bank |
| WhatsApp | JL's own Meta Business number |
| Domain | jlenterprises.in (JL-owned) |

## Structure

```
app/            routes (home, category, product, cart, checkout, service, login)
components/     Header, Footer, ProductCard, ProductBuy, FloatingCart
lib/            catalog (seed), cart context, types, format, supabase clients
supabase/       schema.sql
```
