"use client";

import { useState } from "react";
import { inr } from "@/lib/format";
import { RETURN_FLOW, RETURN_LABEL, type AdminReturn } from "@/lib/admin-demo";
import { ToastHost, useToast } from "@/components/admin/Toast";

const STATUS_CLASS: Record<string, string> = {
  REQUESTED: "bg-pink-100 text-pink-700",
  APPROVED: "bg-blue-100 text-blue-700",
  REJECTED: "bg-slate-200 text-slate-600",
  PICKED_UP: "bg-amber-100 text-amber-700",
  REFUNDED: "bg-green-100 text-green-700",
};

function Row({ r }: { r: AdminReturn }) {
  const [status, setStatus] = useState(r.status);
  const toast = useToast();

  async function update(next: string) {
    setStatus(next);
    const res = await fetch("/api/admin/returns", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id: r.id, status: next }),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      setStatus(r.status);
      toast(`⚠️ ${data.error ?? "Update failed"}`);
      return;
    }
    toast(`#${r.order_no} → ${RETURN_LABEL[next]}${data.note ? ` · ${data.note}` : " · customer notified 💬"}`);
  }

  return (
    <tr className="border-t border-slate-100 align-top">
      <td className="py-3 font-bold text-navy">#{r.order_no}</td>
      <td>
        <b className="text-navy text-[13px]">{r.contact_name}</b>
        <span className="block text-[12px] text-slate-400">{r.phone}</span>
      </td>
      <td>
        <span className={`text-[11px] font-bold px-2 py-0.5 rounded ${r.kind === "CANCEL" ? "bg-slate-100 text-slate-600" : "bg-orange-50 text-orange-600"}`}>
          {r.kind === "CANCEL" ? "Cancellation" : "Return"}
        </span>
      </td>
      <td className="text-slate-600 text-[13px] max-w-[260px]">{r.reason}</td>
      <td className="text-slate-600">{r.payment_mode}</td>
      <td className="text-right font-bold text-navy">{inr(r.total)}</td>
      <td>
        <select
          value={status}
          onChange={(e) => update(e.target.value)}
          className={`text-[12px] font-bold rounded-full px-3 py-1.5 border-0 cursor-pointer ${STATUS_CLASS[status] ?? "bg-slate-100 text-slate-600"}`}
        >
          {RETURN_FLOW.map((s) => <option key={s} value={s}>{RETURN_LABEL[s]}</option>)}
        </select>
      </td>
    </tr>
  );
}

export default function ReturnsTable({ returns }: { returns: AdminReturn[] }) {
  return (
    <ToastHost>
      <div className="bg-white border border-slate-200 rounded-2xl p-5">
        <h3 className="text-navy font-semibold mb-4">{returns.length} Requests</h3>
        {returns.length === 0 ? (
          <p className="text-slate-400 text-sm py-8 text-center">No return or cancellation requests yet. 🎉</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm min-w-[760px]">
              <thead>
                <tr className="text-left text-slate-400 text-[12px] uppercase">
                  <th className="py-2">Order</th><th>Customer</th><th>Type</th><th>Reason</th><th>Payment</th><th className="text-right">Amount</th><th>Status</th>
                </tr>
              </thead>
              <tbody>{returns.map((r) => <Row key={r.id} r={r} />)}</tbody>
            </table>
          </div>
        )}
        <p className="text-[12px] text-slate-400 mt-3">
          Approving a cancellation cancels the order and restores stock. &ldquo;Picked up&rdquo; restores stock for returns.
          &ldquo;Refunded&rdquo; auto-refunds Razorpay payments; COD refunds are transferred manually.
        </p>
      </div>
    </ToastHost>
  );
}
