"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";

export default function LoginPage() {
  const supabase = createClient();
  const router = useRouter();

  const [tab, setTab] = useState<"login" | "signup">("login");
  const [step, setStep] = useState<"enter" | "otp">("enter");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [msg, setMsg] = useState("");
  const [busy, setBusy] = useState(false);

  async function sendOtp() {
    setMsg("");
    if (!email.includes("@")) {
      setMsg("Enter a valid email to receive your OTP.");
      return;
    }
    setBusy(true);
    if (!supabase) {
      // Demo mode (no Supabase keys yet)
      setTimeout(() => { setBusy(false); setStep("otp"); setMsg("Demo mode — enter any 6 digits."); }, 400);
      return;
    }
    const { error } = await supabase.auth.signInWithOtp({
      email,
      options: { shouldCreateUser: true, data: name ? { name } : undefined },
    });
    setBusy(false);
    if (error) { setMsg(error.message); return; }
    setStep("otp");
    setMsg("OTP sent to " + email);
  }

  async function verify() {
    setBusy(true);
    if (!supabase) {
      setTimeout(() => { setBusy(false); router.push("/"); router.refresh(); }, 400);
      return;
    }
    const { error } = await supabase.auth.verifyOtp({ email, token: otp, type: "email" });
    setBusy(false);
    if (error) { setMsg(error.message); return; }
    router.push("/");
    router.refresh();
  }

  return (
    <div className="max-w-[420px] mx-auto my-9 bg-white border border-slate-200 rounded-3xl p-8 animate-fade">
      <h2 className="text-navy text-xl font-bold text-center mb-1.5">Welcome to JL Enterprises</h2>
      <p className="text-center text-slate-400 text-sm mb-5">Login or sign up to track orders &amp; book service</p>

      <div className="flex bg-slate-100 rounded-full p-1 mb-5">
        {(["login", "signup"] as const).map((t) => (
          <button
            key={t}
            onClick={() => { setTab(t); setStep("enter"); }}
            className={`flex-1 py-2.5 rounded-full text-sm font-semibold capitalize ${
              tab === t ? "bg-white text-navy shadow" : "text-slate-500"
            }`}
          >
            {t === "login" ? "Login" : "Sign Up"}
          </button>
        ))}
      </div>

      {step === "otp" ? (
        <>
          <div className="text-center mb-3">
            <span className="text-4xl block mb-2">📩</span>
            <b className="text-navy">Enter the OTP</b>
            <p className="text-slate-400 text-[13px] mt-1">{msg || `Sent to ${email}`}</p>
          </div>
          <input
            value={otp}
            onChange={(e) => setOtp(e.target.value)}
            placeholder="6-digit code"
            inputMode="numeric"
            className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm text-center tracking-[0.4em] mb-3"
          />
          <button onClick={verify} disabled={busy} className="w-full bg-navy disabled:opacity-60 text-white font-bold py-3 rounded-full">
            {busy ? "Verifying…" : "Verify & Continue"}
          </button>
          <button onClick={() => setStep("enter")} className="w-full text-slate-400 text-[13px] mt-3">← Change email</button>
        </>
      ) : (
        <>
          {tab === "signup" && (
            <div className="mb-3">
              <label className="text-[13px] text-slate-400 block mb-1.5">Full Name</label>
              <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Your name" className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm" />
            </div>
          )}
          <div className="mb-1">
            <label className="text-[13px] text-slate-400 block mb-1.5">Email</label>
            <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm" />
          </div>
          {msg && <p className="text-[12px] text-orange-600 mb-2">{msg}</p>}
          <button onClick={sendOtp} disabled={busy} className="w-full bg-navy disabled:opacity-60 text-white font-bold py-3 rounded-full mt-2">
            {busy ? "Sending…" : "Send OTP"}
          </button>
          <p className="text-[11px] text-slate-400 text-center mt-2">
            Phone-OTP activates in Phase 2 once an SMS provider is connected.
          </p>
        </>
      )}

      <div className="text-center mt-4 text-sm">
        or <Link href="/" className="text-brand font-semibold">Continue as Guest →</Link>
      </div>
    </div>
  );
}
