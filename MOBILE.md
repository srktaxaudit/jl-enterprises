# JL Enterprises — Mobile

There are **two** native Expo (React Native) apps, both talking to the same
Spring Boot API (`ecommerce-backend/`):

| App | Directory | Who it's for |
|---|---|---|
| **JL Store** (`slug: jl-store-mobile`) | [`mobile-store/`](mobile-store/) | Customers — browse, cart, checkout (COD), orders, wishlist, addresses, order tracking. Guest browsing allowed. |
| **JL Admin** (`slug: jl-admin-mobile`) | [`mobile/`](mobile/) | Staff — same JWT auth and RBAC as the web admin. |

Both are React Native + Expo + TypeScript with Expo Router, TanStack Query,
Zustand and secure-store token storage (the admin app adds biometrics).
Full setup, run, and troubleshooting notes live in each app's README:
[`mobile-store/README.md`](mobile-store/README.md) and
[`mobile/README.md`](mobile/README.md).

## Run either app (dev)
```bash
cd mobile-store        # or: cd mobile (admin)
npm install            # .npmrc sets legacy-peer-deps=true
npx expo start         # open in Expo Go, or a dev build
```

## Build either app (EAS)
Each app's `eas.json` defines two profiles:
- **preview** → internal Android **APK** (share/sideload for testing)
- **production** → Android **App Bundle (AAB)** for the Play Store

```bash
cd mobile-store        # or: cd mobile (admin)
eas build --profile preview --platform android      # test APK
eas build --profile production --platform android   # Play Store AAB
```

> The customer storefront also exists as a static web site in `frontend/`
> (installable as a PWA from the browser).
