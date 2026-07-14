import Constants from "expo-constants";

/** Backend base URL. Overridable per-build via app.json > expo.extra.apiBaseUrl. */
export const API_BASE_URL: string =
  (Constants.expoConfig?.extra as { apiBaseUrl?: string } | undefined)?.apiBaseUrl ??
  "https://jl-enterprises-api.onrender.com";
