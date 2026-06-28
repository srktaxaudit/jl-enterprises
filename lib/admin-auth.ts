import { cookies } from "next/headers";

export const ADMIN_COOKIE = "jl_admin";

/** True when the current request carries a valid admin session cookie. */
export function isAdmin(): boolean {
  return Boolean(cookies().get(ADMIN_COOKIE)?.value);
}

/** The expected admin password (demo default until ADMIN_PASSWORD is set). */
export function adminPassword(): string {
  return process.env.ADMIN_PASSWORD || "jladmin";
}
