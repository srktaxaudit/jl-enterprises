"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { LogoFull } from "@/components/Logo";

export default function AdminLogin() {
  const [password, setPassword] = useState("");
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);
  const router = useRouter();

  async function submit() {
    setBusy(true);
    setErr("");
    const res = await fetch("/api/admin/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password }),
    });
    setBusy(false);
    if (res.ok) {
      router.push("/admin");
      router.refresh();
    } else {
      setErr("Incorrect password. (Demo password: jladmin)");
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0b2447] to-[#19376d] flex items-center justify-center p-5">
      <div className="bg-white rounded-3xl p-8 w-full max-w-[400px]">
        <div className="flex flex-col items-center mb-6">
          <LogoFull className="w-44 h-auto" />
          <div className="text-[11px] text-slate-400 tracking-widest mt-1">CEO CONTROL CENTRE</div>
        </div>
        <h2 className="text-navy font-bold text-lg mb-1">Admin Login</h2>
        <p className="text-slate-400 text-sm mb-5">Sign in to manage your store</p>
        <label className="text-[13px] text-slate-400 block mb-1.5">Password</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
          placeholder="••••••••"
          className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm mb-1"
        />
        {err && <p className="text-[12px] text-red-500 mb-2">{err}</p>}
        <button onClick={submit} disabled={busy} className="w-full bg-navy disabled:opacity-60 text-white font-bold py-3 rounded-full mt-3">
          {busy ? "Signing in…" : "Sign In"}
        </button>
        <p className="text-[11px] text-slate-400 text-center mt-4">Demo password: <b>jladmin</b> · set ADMIN_PASSWORD to change</p>
      </div>
    </div>
  );
}
