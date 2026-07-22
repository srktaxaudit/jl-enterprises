# JL Admin — Mobile App (React Native + Expo)

Native Android/iOS admin app for JL Enterprises. Talks to the **same** Spring Boot
API (`https://jl-enterprises-api.onrender.com`) with the same JWT auth, RBAC and
business rules as the web admin — but with a purpose-built mobile UI.

> **Status: all admin modules built.** Auth + biometrics, Dashboard, Orders and
> Products, plus every module in the More hub: Inventory, Offers, Customers,
> Reviews, Service, Exchange, Staff, Team/Roles, Activity Logs, Settings, WhatsApp,
> Branding, and the full Accounting suite (Chart of Accounts, Journal, Invoices &
> Bills, Billing, Financial Reports, GST, Outstanding & Cashflow, Ledgers,
> Import/Export). Each is a data hook (`src/features/<key>/hooks.ts`) + a screen
> (`app/<key>.tsx`) following the Orders/Products pattern. Accounting screens are
> read-focused; heavy data entry (journal posting, invoice creation, file
> import/export) stays on the web admin.

## Stack (chosen for maintainability + your JS skills)

| Concern | Choice | Why |
|---|---|---|
| Language | **TypeScript** | Type-safe, matches the web codebase |
| Framework | **Expo (React Native)** | Managed workflow, OTA updates, EAS builds, huge ecosystem |
| Navigation | **Expo Router** | File-based, typed routes, native stack + tabs |
| Server state / cache | **TanStack Query** | Caching, retries, infinite scroll, pull-to-refresh, offline-ready |
| Client state | **Zustand** | Tiny, no boilerplate (used for the auth session) |
| Networking | **axios** | Interceptors for bearer token + single-flight JWT refresh |
| Secure storage | **expo-secure-store** | Tokens in the OS keychain/keystore (encrypted) |
| Biometrics | **expo-local-authentication** | Face ID / fingerprint unlock gate |
| Push | **expo-notifications** | Device token + notification handling (wiring TODO) |
| Images | **expo-image** / **expo-image-picker** | Fast caching + upload |
| Forms | **react-hook-form + zod** | Declarative validation, shared schemas |

## Architecture (clean / modular)

```
mobile/
  app/                         # Expo Router routes (thin screens only)
    _layout.tsx                # providers (QueryClient, Theme), session restore, root Stack
    index.tsx                  # auth gate → redirects to (app) or login
    login.tsx
    (app)/                     # authenticated area (bottom tabs) — guarded + biometric gate
      _layout.tsx              # Tabs: Dashboard · Orders · Products · More
      index.tsx                # Dashboard
      orders.tsx  products.tsx  more.tsx
    order/[id].tsx             # order detail (pushes over the tabs)
  src/
    core/                      # cross-cutting infrastructure
      api/        client.ts (axios + interceptors + envelope unwrap), queryClient.ts
      auth/       authStore.ts (zustand), tokenStore.ts (secure), rbac.ts, biometric.ts
      config/     env.ts
      navigation/ modules.ts   # the 15-module registry + RBAC (mirrors admin-shell.js)
      theme/      tokens.ts, ThemeProvider.tsx (system dark mode)
      types.ts                 # ApiResponse / PageResponse / AuthUser
    features/                  # one folder per domain: data hooks + types
      dashboard/ orders/ products/
    shared/                    # reusable UI + utils
      components/ui.tsx        # Screen, Button, Card, TextField, StatusBadge, Empty/Loading/Error
      format.ts                # inr(), dateTime()
```

Screens stay thin: they consume a feature hook (which owns the API call + cache)
and compose shared components. API access is centralised in `core/api/client.ts`
— nothing calls `fetch`/`axios` directly.

## Run it

```bash
cd mobile
npm install
npx expo start            # press a for Android emulator, i for iOS (macOS), or scan the QR in Expo Go
```

The API base URL is in `app.json` → `expo.extra.apiBaseUrl` (defaults to the
Render backend). Log in with a staff account; RBAC hides modules you can’t access.

## Add a module (the pattern)

1. `src/features/<name>/hooks.ts` — a `useQuery`/`useInfiniteQuery` calling `apiGet(...)`
   (+ mutations with `apiPost/apiPatch`). Copy `features/orders/hooks.ts`.
2. `app/(app)/<name>.tsx` — a screen using `Screen`, a `FlatList` (pull-to-refresh +
   `onEndReached`), and the Loading/Empty/Error states. Copy `orders.tsx`.
3. Flip `built: true` on that entry in `src/core/navigation/modules.ts` and point its
   `route` at the new screen. It appears (RBAC-filtered) in the More hub automatically.

## Production checklist (follow-ups)

Done already: branded icon/splash/adaptive assets are in `assets/` and wired in
`app.json`; the order-status PATCH sends `status` as a query param matching
`AdminOrderController`; CI typechecks the app on every push (`package-lock.json`
is committed — keep it updated when deps change).

- **Offline caching** — wrap the QueryClient with `@tanstack/query-async-storage-persister` + AsyncStorage.
- **Push notifications** — deliberately NOT included yet (the unused `expo-notifications`
  dep/plugin was removed to keep store review clean). To add: reinstall it, restore the
  plugin in `app.json`, register the device token, and add a backend endpoint to store
  it + send on order/low-stock/exchange events (backend has a NotificationService).
- **Image upload** — deliberately NOT included yet (unused `expo-image-picker` + iOS
  camera/photo permission strings removed — undeclared-use permissions are a common
  App Store rejection). To add: reinstall, restore the plugin + permission strings,
  then multipart POST to `/api/v1/products/{id}/images` (reuse the web pattern).
- **Builds & stores** — `eas init` (link a projectId), fill `eas.json` `submit.production`,
  then `eas build -p android|ios` and `eas submit`. App IDs: `com.jlenterprises.admin`.

## Security notes

- Tokens are stored **only** in `expo-secure-store` (OS keychain/keystore), never AsyncStorage.
- A single-flight refresh interceptor renews the access token on 401; a failed refresh drops to the login screen.
- RBAC (`core/auth/rbac.ts`) mirrors the web `admin.js`/`admin-shell.js` rules exactly, so the same roles see the same modules. The **backend `@PreAuthorize` guards remain the source of truth** — the app UI just hides what a role can’t use.
- Optional biometric unlock gates the authenticated area on cold start.
