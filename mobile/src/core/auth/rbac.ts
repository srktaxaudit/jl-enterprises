import type { AuthUser } from "@/core/types";

// Mirrors the web admin's RBAC (frontend/admin.js) so access rules stay identical.
const SUPER_ROLES = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN"];

export const isSuper = (u?: AuthUser | null): boolean =>
  !!u?.roles?.some((r) => SUPER_ROLES.includes(r));

/** True if the user is super/admin OR holds any of the given bare role names. */
export function hasRole(u: AuthUser | null | undefined, ...roleNames: string[]): boolean {
  if (!u) return false;
  if (isSuper(u)) return true;
  const have = u.roles || [];
  return roleNames.some((rn) => have.includes("ROLE_" + rn) || have.includes(rn));
}

/**
 * Menu-visibility rule, identical to admin-shell.js `visible()`:
 *   undefined  → all staff
 *   "@admin"   → super/admin only
 *   "A,B"      → super/admin OR MANAGER OR any listed role
 */
export function canSee(u: AuthUser | null | undefined, rule?: string): boolean {
  if (!u) return false;
  if (!rule) return true;
  if (rule === "@admin") return isSuper(u);
  return isSuper(u) || hasRole(u, "MANAGER", ...rule.split(","));
}

export const displayName = (u?: AuthUser | null): string =>
  !u ? "" : [u.firstName, u.lastName].filter(Boolean).join(" ").trim() || u.email;

export const initials = (u?: AuthUser | null): string =>
  displayName(u)
    .split(/[\s@.]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase())
    .join("") || "JL";
