# JL Enterprises — Mobile

The native mobile app is the **Expo (React Native) admin app** in [`mobile/`](mobile/).
It talks to the same Spring Boot API (`ecommerce-backend/`) with the same JWT auth
and RBAC as the web admin.

- App: **JL Admin** (`slug: jl-admin-mobile`) — React Native + Expo + TypeScript,
  Expo Router, TanStack Query, Zustand, secure-store token storage, biometrics.
- Full setup, run, and troubleshooting notes live in [`mobile/README.md`](mobile/README.md).

## Run it (dev)
```bash
cd mobile
npm install            # .npmrc sets legacy-peer-deps=true
npx expo start         # open in Expo Go, or a dev build
```

## Build it (EAS)
`mobile/eas.json` defines two profiles:
- **preview** → internal Android **APK** (share/sideload for testing)
- **production** → Android **App Bundle (AAB)** for the Play Store

```bash
cd mobile
eas build --profile preview --platform android      # test APK
eas build --profile production --platform android   # Play Store AAB
```

> The customer storefront is the static site in `frontend/` (also installable as a
> PWA from the browser). This document covers only the native admin app.
