"use client";

import { useState } from "react";
import { inr } from "@/lib/format";
import { STATUS_FLOW, STATUS_LABEL, type AdminOrder } from "@/lib/admin-demo";
import { ToastHost, useToast } from "@/components/admin/Toast";

const STATUS_CLASS: Record<string, string> = {
  NEW: "bg-pink-100 text-pink-700",
  PACKED: "bg-amber-100 text-amber-700",
  OUT_FOR_DELIVERY: "bg-blue-100 text-blue-700",
  DELIVERED: "bg-green-100 text-green-700",
};

function Row({ o }: { o: AdminOrder }) {
  const [status, setStatus] = useState(o.status);
  const toast = useToast();

  async function update(next: string) {
    setStatus(next);
    await fetch("/api/admin/orders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ id: o.id, status: next, orderNo: o.order_no, phone: "9876543210" }),
    });
    if (next !== "NEW") toast(`#${o.order_no} → ${STATUS_LABEL[next]} · WhatsApp sent 💬`);
  }

  return (
    <tr className="border-t border-slate-100">
      <td className="py-3 font-bold text-navy">#{o.order_no}</td>
      <td><b className="text-navy text-[13px]">{o.contact_name}</b><span className="block text-[12px] text-slate-400">{o.city}</span></td>
      <td className="text-slate-600">{o.product}</td>
      <td className="text-slate-600">{o.payment_mode}</td>
      <td className="text-right font-bold text-navy">{inr(o.total)}</td>
      <td>
        <select
          value={status}
          onChange={(e) => update(e.target.value)}
          className={`text-[12px] font-bold rounded-full px-3 py-1.5 border-0 cursor-pointer ${STATUS_CLASS[status] ?? "bg-slate-100 text-slate-600"}`}
        >
          {STATUS_FLOW.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
        </select>
      </td>
    </tr>
  );
}

export default function OrdersTable({ orders }: { orders: AdminOrder[] }) {
  return (
    <ToastHost>
      <div className="bg-white border border-slate-200 rounded-2xl p-5">
        <h3 className="text-navy font-semibold mb-4">{orders.length} Orders</h3>
        <div className="overflow-x-auto"><table className="w-full text-sm min-w-[600px]">
          <thead><tr className="text-left text-slate-400 text-[12px] uppercase">
            <th className="py-2">Order</th><th>Customer</th><th>Product</th><th>Payment</th><th className="text-right">Amount</th><th>Status</th>
          </tr></thead>
          <tbody>{orders.map((o) => <Row key={o.id} o={o} />)}</tbody>
        </table></div>
        <p className="text-[12px] text-slate-400 mt-3">Change a status to advance the order — the customer gets a WhatsApp update automatically.</p>
      </div>
    </ToastHost>
  );
}
