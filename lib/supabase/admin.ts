import { createClient } from "@supabase/supabase-js";

/**
 * Service-role Supabase client for SERVER-ONLY writes (orders, etc.).
 * Bypasses RLS — never import this into client components.
 * Returns null until the service-role key is set.
 */
export function createAdminClient() {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!url || !key) return null;
  return createClient(url, key, { auth: { persistSession: false } });
}
