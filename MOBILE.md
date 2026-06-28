# JL Enterprises — Mobile Apps

Two ways the store is available on phones:

1. **PWA (installable today)** — works the moment the site is live, no store needed.
2. **Native apps (Play Store / App Store)** — via Capacitor, wrapping the live site.
   Two builds: **customer app** and **staff app**.

---

## 1. PWA (Progressive Web App) — zero extra work

Already wired up:
- `app/manifest.ts` → app name, icons, theme (`/manifest.webmanifest`)
- `public/icon.svg` → app icon · `public/sw.js` → service worker (offline shell)
- Apple + theme-color meta in `app/layout.tsx`

**Customers**: open `jlenterprises.in` on a phone → browser menu → *Add to Home Screen*.
**Staff**: open `jlenterprises.in/admin` → *Add to Home Screen* → opens straight to the admin.

> For best store-quality icons later, export `public/icon.svg` to PNGs
> (192×192, 512×512, 180×180 apple-touch) and add them to the manifest.

---

## 2. Native apps with Capacitor

`capacitor.config.ts` is already set to load the deployed site
(`server.url = https://jlenterprises.in`). This is the right pattern for a
server-rendered Next.js app — the native shell shows the live, always-current site.

### One-time setup
```bash
npm install            # installs @capacitor/core + cli (already in package.json)
npm install @capacitor/android @capacitor/ios
npx cap add android    # creates android/  (needs Android Studio + JDK 17)
npx cap add ios        # creates ios/      (macOS + Xcode only)
npx cap sync
```

### Build & run
```bash
npx cap open android   # opens Android Studio → Run / Build APK/AAB
npx cap open ios       # opens Xcode → Run / Archive
```

### App icon & splash
```bash
npm install -D @capacitor/assets
# put a 1024×1024 logo at resources/icon.png + resources/splash.png
npx capacitor-assets generate
```
(Export `public/icon.svg` at 1024×1024 for `resources/icon.png`.)

---

## 3. Staff / team app (second build)

The client asked for mobile features on the staff side too. The admin panel is
already mobile-responsive (drawer nav), so the same wrap works:

1. Copy `capacitor.config.ts` → `capacitor.staff.config.ts`:
   ```ts
   appId: "com.jlenterprises.staff",
   appName: "JL Staff",
   server: { url: "https://jlenterprises.in/admin/login" },
   ```
2. Build it as a separate app (use `--config capacitor.staff.config.ts` with cap
   commands, or a second project folder) and publish as **“JL Staff”**.

Staff then get a home-screen app that opens directly to the admin login and can
manage orders, inventory, offers and WhatsApp broadcasts from their phone.

---

## 4. Store submission checklist

| Item | Customer app | Staff app |
|---|---|---|
| App ID | com.jlenterprises.store | com.jlenterprises.staff |
| Name | JL Enterprises | JL Staff |
| Icon (1024²) | from icon.svg | same |
| Privacy policy URL | jlenterprises.in/privacy | same |
| Google Play account | $25 one-time | (same account) |
| Apple Developer | $99/yr (iOS only) | (same account) |

> Razorpay in a webview: enable UPI/intent flows; test the payment journey on a
> real device before submitting. WhatsApp deep links (`wa.me`) open the WhatsApp app.
