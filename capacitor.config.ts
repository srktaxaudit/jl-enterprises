import type { CapacitorConfig } from "@capacitor/cli";

// ─── JL Enterprises — Customer app ──────────────────────────────────
// Wraps the deployed storefront in a native Android/iOS shell.
// (Next.js uses server rendering + APIs, so we load the live site rather
//  than a static export — same pattern as the SRK client app.)
//
// For the STAFF app, copy this file to capacitor.staff.config.ts and use:
//   appId: "com.jlenterprises.staff", appName: "JL Staff",
//   server.url: "https://jlenterprises.in/admin/login"
// See MOBILE.md for the full two-app build flow.

const config: CapacitorConfig = {
  appId: "com.jlenterprises.store",
  appName: "JL Enterprises",
  webDir: "public",
  server: {
    url: "https://jlenterprises.in",
    cleartext: false,
  },
  backgroundColor: "#0b2447",
};

export default config;
