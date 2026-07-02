import crypto from "crypto";
import { cookies } from "next/headers";

export const ADMIN_COOKIE = "jl_admin";

/** Demo-only fallback — never used when NODE_ENV is "production". */
const DEMO_PASSWORD = "jladmin";

/** The expected admin password. Returns null in production when
 *  ADMIN_PASSWORD isn't set, which disables admin login entirely. */
export function adminPassword(): string | null {
  if (process.env.ADMIN_PASSWORD) return process.env.ADMIN_PASSWORD;
  return process.env.NODE_ENV === "production" ? null : DEMO_PASSWORD;
}

/** Deterministic HMAC session token derived from the admin password —
 *  the cookie value must match this exactly, so it can't be forged by
 *  simply setting jl_admin to any value. Rotates when the password does. */
export function adminSessionToken(): string | null {
  const pw = adminPassword();
  if (!pw) return null;
  return crypto.createHmac("sha256", pw).update("jl-admin-session-v1").digest("hex");
}

function safeEqual(a: string, b: string): boolean {
  const ba = Buffer.from(a);
  const bb = Buffer.from(b);
  return ba.length === bb.length && crypto.timingSafeEqual(ba, bb);
}

/** True when the current request carries a valid, signed admin session. */
export function isAdmin(): boolean {
  const cookie = cookies().get(ADMIN_COOKIE)?.value;
  const expected = adminSessionToken();
  return Boolean(cookie && expected && safeEqual(cookie, expected));
}

/** Timing-safe password check for the login route. */
export function checkAdminPassword(candidate: string): boolean {
  const expected = adminPassword();
  return Boolean(expected && safeEqual(candidate, expected));
}
