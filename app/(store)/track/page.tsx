"use client";

import Link from "next/link";
import { useState } from "react";
import { inr } from "@/lib/format";

const STEPS = [
  { key: "NEW", label: "Order placed", emoji: "🧾" },
  { key: "PACKED", label: "Packed", emoji: "📦" },
  { key: "OUT_FOR_DELIVERY", label: "Out for delivery", emoji: "🚚" },
  { key: "DELIVERED", label: "Delivered", emoji: "✅" },
];

type TrackedOrder = {
  orderNo: string;
  status: string;
  paymentMode: string;
  paymentStatus: string;
  total: number;
  createdAt: string;
  deliveryDate: string | null;
  deliverySlot: string | null;
  city: string | null;
  items: { name: string; brand: string; qty: number; price: number }[];
  returnRequest: { kind: string; status: string; createdAt: string } | null;
};

export default function TrackPage() {
  const [orderNo, setOrderNo] = useState("");
  const [phone, setPhone] = useState("");
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState("");
  const [demo, setDemo] = useState(false);
  const [order, setOrder] = useState<TrackedOrder | null>(null);

  // return / cancel request form
  const [reqKind, setReqKind] = useState<"RETURN" | "CANCEL" | null>(null);
  const [reason, setReason] = useState("");
  const [reqMsg, setReqMsg] = useState("");
  const [reqBusy, setReqBusy] = useState(false);

  async function lookup(e: React.FormEvent) {
    e.preventDefault();
    setMsg("");
    setOrder(null);
    setReqKind(null);
    setReqMsg("");
    setBusy(true);
    try {
      const res = await fetch("/api/track", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ orderNo, phone }),
      });
      const data = await res.json();
      if (res.ok && data.order) {
        setOrder(data.order);
        setDemo(Boolean(data.demo));
      } else {
        setMsg(data.error || "Could not find that order.");
      }
    } catch {
      setMsg("Could not reach the server — please try again.");
    }
    setBusy(false);
  }

  async function submitRequest() {
    if (!order || !reqKind) return;
    setReqMsg("");
    setReqBusy(true);
    try {
      const res = await fetch("/api/returns", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ orderNo: order.orderNo, phone, kind: reqKind, reason }),
      });
      const data = await res.json();
      if (res.ok && data.ok) {
        setOrder({
          ...order,
          returnRequest: { kind: reqKind, status: "REQUESTED", createdAt: new Date().toISOString() },
        });
        setReqKind(null);
        setReason("");
      } else {
        setReqMsg(data.error || "Could not submit the request — please call us.");
      }
    } catch {
      setReqMsg("Could not reach the server — please try again.");
    }
    setReqBusy(false);
  }

  const stepIdx = order ? STEPS.findIndex((s) => s.key === order.status) : -1;
  const cancelled = order?.status === "CANCELLED";
  const canCancel = order && ["NEW", "PACKED"].includes(order.status) && !order.returnRequest;
  const canReturn = order && order.status === "DELIVERED" && !order.returnRequest;

  const RETURN_LABELS: Record<string, string> = {
    REQUESTED: "requested — we'll confirm within 1 working day",
    APPROVED: "approved — pickup will be arranged",
    REJECTED: "not approved — we'll call you to explain",
    PICKED_UP: "picked up — refund in process",
    REFUNDED: "refunded — reflects in 5–7 business days",
  };

  return (
    <div className="max-w-[720px] mx-auto px-5 animate-fade">
      <h2 className="text-2xl text-navy font-bold mt-6 mb-1">🚚 Track Your Order</h2>
      <p className="text-slate-400 text-sm mb-5">
        Enter your order number and the phone number you used at checkout.
      </p>

      <form onSubmit={lookup} className="bg-white border border-slate-200 rounded-2xl p-5 mb-5 flex gap-3 flex-wrap">
        <div className="flex-1 min-w-[140px]">
          <label className="text-[13px] text-slate-400 block mb-1.5">Order number</label>
          <input
            value={orderNo}
            onChange={(e) => setOrderNo(e.target.value)}
            placeholder="JL123456"
            required
            className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm uppercase"
          />
        </div>
        <div className="flex-1 min-w-[140px]">
          <label className="text-[13px] text-slate-400 block mb-1.5">Phone</label>
          <input
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="10-digit mobile"
            required
            inputMode="tel"
            className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm"
          />
        </div>
        <button
          type="submit"
          disabled={busy}
          className="self-end bg-navy disabled:opacity-60 text-white font-bold px-6 py-2.5 rounded-full text-sm"
        >
          {busy ? "Checking…" : "Track"}
        </button>
        {msg && <p className="w-full text-[13px] text-orange-600">{msg}</p>}
      </form>

      {order && (
        <div className="bg-white border border-slate-200 rounded-2xl p-5 mb-8">
          {demo && (
            <p className="text-[12px] text-orange-600 mb-3">
              Demo mode — sample order shown until the store database is connected.
            </p>
          )}
          <div className="flex justify-between items-baseline flex-wrap gap-2 mb-4">
            <h3 className="text-navy font-bold">Order #{order.orderNo}</h3>
            <span className="text-[13px] text-slate-400">
              Placed {new Date(order.createdAt).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}
            </span>
          </div>

          {cancelled ? (
            <div className="bg-red-50 border border-red-200 text-red-600 rounded-xl px-4 py-3 text-sm font-semibold mb-4">
              ❌ This order was cancelled.
            </div>
          ) : (
            <div className="flex items-start mb-5">
              {STEPS.map((s, i) => {
                const done = i <= stepIdx;
                return (
                  <div key={s.key} className="flex-1 text-center relative">
                    {i > 0 && (
                      <div className={`absolute top-4 right-1/2 w-full h-1 -z-0 ${i <= stepIdx ? "bg-green-500" : "bg-slate-200"}`} />
                    )}
                    <div className={`relative z-[1] w-8 h-8 mx-auto rounded-full flex items-center justify-center text-[15px] ${done ? "bg-green-500" : "bg-slate-200"}`}>
                      {s.emoji}
                    </div>
                    <div className={`text-[11px] mt-1.5 font-semibold ${done ? "text-green-600" : "text-slate-400"}`}>
                      {s.label}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {(order.deliveryDate || order.deliverySlot) && !cancelled && order.status !== "DELIVERED" && (
            <p className="text-sm text-slate-600 mb-3">
              🗓️ Preferred slot: <b>{order.deliveryDate}</b>{order.deliverySlot ? `, ${order.deliverySlot}` : ""}
            </p>
          )}

          {order.items.map((i, idx) => (
            <div key={idx} className="flex justify-between py-1.5 text-[13px] text-slate-600 border-t border-slate-100 first:border-0">
              <span>{i.name} ×{i.qty}</span>
              <span>{inr(i.price * i.qty)}</span>
            </div>
          ))}
          <div className="flex justify-between border-t border-slate-200 mt-2 pt-3 font-extrabold text-navy">
            <span>Total ({order.paymentMode}{order.paymentStatus === "PAID" ? " · paid" : ""})</span>
            <span>{inr(order.total)}</span>
          </div>

          {order.returnRequest && (
            <div className="bg-amber-50 border border-amber-200 text-amber-700 rounded-xl px-4 py-3 text-sm mt-4">
              🔁 {order.returnRequest.kind === "CANCEL" ? "Cancellation" : "Return"} request{" "}
              {RETURN_LABELS[order.returnRequest.status] ?? order.returnRequest.status.toLowerCase()}.
            </div>
          )}

          {(canCancel || canReturn) && !reqKind && (
            <div className="flex gap-2 mt-4">
              <button
                onClick={() => setReqKind(canCancel ? "CANCEL" : "RETURN")}
                className="border border-slate-300 text-slate-600 hover:border-navy hover:text-navy px-4 py-2 rounded-full text-sm font-semibold"
              >
                {canCancel ? "Request Cancellation" : "Request Return"}
              </button>
            </div>
          )}

          {reqKind && (
            <div className="border border-slate-200 rounded-xl p-4 mt-4">
              <label className="text-[13px] text-slate-400 block mb-1.5">
                Why do you want to {reqKind === "CANCEL" ? "cancel" : "return"} this order?
              </label>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={2}
                maxLength={500}
                placeholder="e.g. Unit arrived with a damaged panel"
                className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm"
              />
              {reqMsg && <p className="text-[12px] text-orange-600 mt-1">{reqMsg}</p>}
              <div className="flex gap-2 mt-2">
                <button
                  onClick={submitRequest}
                  disabled={reqBusy || reason.trim().length < 5}
                  className="bg-navy disabled:opacity-50 text-white font-bold px-5 py-2 rounded-full text-sm"
                >
                  {reqBusy ? "Submitting…" : "Submit Request"}
                </button>
                <button onClick={() => setReqKind(null)} className="text-slate-400 text-sm px-2">
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      <p className="text-[13px] text-slate-400 text-center mb-8">
        Can&rsquo;t find your order? See the{" "}
        <Link href="/refund-policy" className="text-brand">Refund Policy</Link> or call us — numbers in the footer.
      </p>
    </div>
  );
}
