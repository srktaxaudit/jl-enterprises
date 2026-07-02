"use client";

import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";

export default function SignOutButton() {
  const router = useRouter();

  async function signOut() {
    const supabase = createClient();
    if (supabase) await supabase.auth.signOut();
    router.push("/");
    router.refresh();
  }

  return (
    <button
      onClick={signOut}
      className="border border-slate-200 text-slate-500 hover:text-navy hover:border-navy px-4 py-2 rounded-full text-sm font-semibold"
    >
      Sign out
    </button>
  );
}
