# JL Store — Customer Mobile App

The native **customer shopping app** for JL Enterprises (Home Appliances &
Furniture, Thoothukudi). React Native + **Expo** + TypeScript, talking to the
same Spring Boot API (`ecommerce-backend/`) as the web storefront.

> The **admin** mobile app lives separately in [`mobile/`](../mobile/). This app
> is the customer-facing counterpart — the two share conventions (Expo Router,
> TanStack Query, Zustand, secure-store tokens) but are independent apps with
> their own bundle IDs and token storage.

## What's in the app

| Area | Screens |
|---|---|
| **Browse** (no sign-in needed) | Home (banners, categories, featured), Shop (search, category & sort filters, infinite scroll), Product detail (gallery, EMI info, reviews, related products) |
| **Buy** | Cart (quantity stepper, badge on the tab), Checkout (address picker, coupon validation, COD), Order confirmation |
| **After the sale** | My Orders (paginated), Order detail (cancel before shipping, request return after delivery), public order tracking by number + phone |
| **Account** | Login (email **or** mobile number), sign-up, profile card, address book CRUD (+ default address), wishlist |

Guests can browse everything; the app prompts for sign-in only where the API
requires it (cart, wishlist, checkout, orders).

### Payments

**COD is the only payment method offered** — it's the one method fully live on
the backend. Razorpay is wired server-side; when the mobile checkout should
support it, integrate `react-native-razorpay` and add `RAZORPAY` to
`PAYMENT_METHODS` in `app/checkout.tsx`.

## Architecture

```
mobile-store/
├── app/                    # Expo Router file-based routes
│   ├── _layout.tsx         # Providers (query, theme, safe area) + stack
│   ├── (tabs)/             # Home · Shop · Cart · Orders · Account
│   ├── product/[slug].tsx  # Product detail
│   ├── order/[id].tsx      # Order detail
│   ├── checkout.tsx  login.tsx  signup.tsx
│   └── addresses.tsx  wishlist.tsx  track-order.tsx
└── src/
    ├── core/               # api client (JWT + single-flight refresh),
    │                       # auth store (guest/authed), theme, types
    ├── features/           # TanStack Query hooks per domain:
    │                       # catalog, cart, wishlist, orders, addresses
    └── shared/             # UI kit, ProductCard, SignInPrompt, formatting
```

- **API base URL** comes from `app.json > expo.extra.apiBaseUrl`
  (defaults to the Render deployment). Point it at `http://<your-ip>:8081`
  for local backend development.
- **Tokens** are stored in the OS keychain via `expo-secure-store` under
  `jl_store_*` keys (never colliding with the admin app's `jl_admin_*`).
- A 401 triggers one shared refresh; if that fails the user quietly becomes a
  guest and is re-prompted at the next auth-only action.

## Run it (dev)

```bash
cd mobile-store
npm install            # .npmrc sets legacy-peer-deps=true
npx expo start         # open in Expo Go, or a dev build
```

## Build it (EAS)

`eas.json` defines the same two profiles as the admin app:

```bash
cd mobile-store
eas build --profile preview --platform android      # internal test APK
eas build --profile production --platform android   # Play Store AAB
```

- Android package: `com.jlenterprises.store`
- iOS bundle ID: `com.jlenterprises.store`

> The app icons in `assets/` are copied from the admin app as placeholders —
> replace them with customer-branded artwork before a store release.
