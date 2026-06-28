"use client";

import { useState } from "react";
import { CONTACTS, telHref, waHref, prettyPhone } from "@/lib/contact";

const APPLIANCES = [
  ["AC", "❄️"], ["Fridge", "🧊"], ["Washing M.", "🌀"], ["TV", "📺"],
];

export default function ServicePage() {
  const [appliance, setAppliance] = useState("AC");
  const [done, setDone] = useState(false);

  return (
    <div className="max-w-[1180px] mx-auto px-5 animate-fade">
      <div className="text-[13px] text-slate-400 my-4">Home / Book Service</div>
      <div className="grid md:grid-cols-[1fr_340px] gap-6 mb-8">
        <div className="bg-white border border-slate-200 rounded-2xl p-6">
          <h3 className="text-navy font-semibold text-lg mb-4">🔧 Book a Service / AMC</h3>

          {done ? (
            <div className="text-center py-10">
              <span className="text-5xl block mb-3">✅</span>
              <b className="text-navy text-lg">Service Requested!</b>
              <p className="text-slate-500 text-sm mt-1">Our team will confirm your slot on WhatsApp.</p>
            </div>
          ) : (
            <>
              <p className="text-sm text-slate-400 mb-2">Select appliance</p>
              <div className="grid grid-cols-4 gap-3 mb-4">
                {APPLIANCES.map(([name, e]) => (
                  <button
                    key={name}
                    onClick={() => setAppliance(name)}
                    className={`rounded-xl p-4 text-center border-2 ${
                      appliance === name ? "border-orange bg-orange-50" : "border-slate-200 bg-white"
                    }`}
                  >
                    <span className="text-2xl block mb-1.5">{e}</span>
                    <b className="text-navy text-sm">{name}</b>
                  </button>
                ))}
              </div>

              <Field label="Service type">
                <select className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm">
                  <option>Repair</option><option>Installation</option>
                  <option>AMC (Annual Maintenance)</option><option>General Service</option>
                </select>
              </Field>
              <div className="flex gap-3">
                <Field label="Preferred date"><input type="date" className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm" /></Field>
                <Field label="Time slot">
                  <select className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm">
                    <option>10 AM – 12 PM</option><option>12 – 2 PM</option><option>2 – 4 PM</option><option>4 – 6 PM</option>
                  </select>
                </Field>
              </div>
              <Field label="Address / Area"><input placeholder="Thoothukudi, Tamil Nadu" className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm" /></Field>

              <button onClick={() => setDone(true)} className="w-full bg-gradient-to-br from-orange to-amber text-white font-bold py-3.5 rounded-full mt-2">
                Book Service →
              </button>
            </>
          )}
        </div>

        <div className="bg-white border border-slate-200 rounded-2xl p-5 h-fit">
          <h3 className="text-navy font-semibold mb-3">Why book with JL?</h3>
          <ul className="flex flex-col gap-3 text-sm text-slate-600">
            <li>👨‍🔧 In-house &amp; verified partner technicians</li>
            <li>📍 Service anywhere in Tamil Nadu</li>
            <li>💬 WhatsApp confirmation &amp; reminders</li>
            <li>💰 Transparent pricing, no hidden charges</li>
            <li>🛡️ AMC plans from ₹1,499/year</li>
          </ul>
        </div>

        <div className="bg-white border border-slate-200 rounded-2xl p-5 h-fit">
          <h3 className="text-navy font-semibold mb-1">Prefer to call?</h3>
          <p className="text-slate-400 text-[13px] mb-3">Reach our Service Team directly</p>
          <a href={telHref(CONTACTS.service.phone)} className="flex items-center gap-3 bg-teal-50 border border-teal-100 rounded-xl px-4 py-3 mb-2">
            <span className="text-xl">📞</span>
            <div><b className="text-navy block">{prettyPhone(CONTACTS.service.phone)}</b><span className="text-[12px] text-slate-400">Tap to call Service Team</span></div>
          </a>
          <a href={waHref(CONTACTS.service.phone, "Hi, I'd like to book a service.")} target="_blank" className="flex items-center gap-3 bg-green-50 border border-green-100 rounded-xl px-4 py-3">
            <span className="text-xl">💬</span>
            <div><b className="text-navy block">WhatsApp Service</b><span className="text-[12px] text-slate-400">Chat with the team</span></div>
          </a>
        </div>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="mb-3 flex-1">
      <label className="text-[13px] text-slate-400 block mb-1.5">{label}</label>
      {children}
    </div>
  );
}
