"use client";

import { useState } from "react";

export default function Broadcast() {
  const [audience, setAudience] = useState("all");
  const [message, setMessage] = useState(
    "🔥 JL Enterprises Summer Sale! Up to 40% off on ACs + free installation. Exchange your old AC for extra ₹3,000 off. Shop now 👉 jlenterprises.in"
  );
  const [result, setResult] = useState("");
  const [busy, setBusy] = useState(false);

  async function send() {
    setBusy(true);
    setResult("");
    const res = await fetch("/api/whatsapp/broadcast", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message, audience }),
    });
    const data = await res.json();
    setBusy(false);
    if (data.demo) setResult("✅ Queued (demo) — connect Supabase + WhatsApp to send to real customers.");
    else if (data.sent !== undefined) setResult(`✅ Sent to ${data.sent}/${data.recipients} customers.`);
    else setResult(data.error ?? "Done.");
  }

  return (
    <div className="grid md:grid-cols-[1.4fr_1fr] gap-5">
      <div className="bg-white border border-slate-200 rounded-2xl p-5">
        <h3 className="text-navy font-semibold mb-4">New Broadcast</h3>
        <label className="text-[12px] text-slate-400 block mb-1.5">Audience</label>
        <select value={audience} onChange={(e) => setAudience(e.target.value)} className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm mb-3">
          <option value="all">All customers</option>
          <option value="ac">AC buyers</option>
          <option value="repeat">Repeat buyers</option>
        </select>
        <label className="text-[12px] text-slate-400 block mb-1.5">Message</label>
        <textarea value={message} onChange={(e) => setMessage(e.target.value)} rows={5} className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm mb-3" />
        <button onClick={send} disabled={busy} className="bg-navy disabled:opacity-60 text-white font-bold px-5 py-2.5 rounded-lg">
          {busy ? "Sending…" : "📣 Send Broadcast"}
        </button>
        {result && <p className="text-[13px] text-green-600 mt-3">{result}</p>}
      </div>
      <div className="bg-white border border-slate-200 rounded-2xl p-5">
        <h3 className="text-navy font-semibold mb-4">Recent Campaigns</h3>
        {[
          ["Monsoon AC Offer", "1,240 sent · 64% read · 38 orders"],
          ["Furniture Fest", "2,910 sent · 58% read · 22 orders"],
          ["Exchange Bonus", "812 sent · 71% read · 19 orders"],
        ].map(([t, s]) => (
          <div key={t} className="flex items-center gap-3 py-2.5 border-t border-slate-100 first:border-0">
            <span className="w-9 h-9 rounded-lg bg-green-100 text-green-700 flex items-center justify-center">📣</span>
            <div><b className="text-navy text-[13px] block">{t}</b><span className="text-[12px] text-slate-400">{s}</span></div>
          </div>
        ))}
      </div>
    </div>
  );
}
