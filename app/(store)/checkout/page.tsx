"use client";

import Link from "next/link";
import { useRef, useState } from "react";
import { useCart } from "@/lib/cart";
import { inr } from "@/lib/format";
import {
  DELIVERY_SLOTS,
  EXCHANGE_BONUS,
  deliveryDateOption,
  validateContact,
  validateEmail,
} from "@/lib/checkout";

const PAYMENTS = ["🟢 Cash on Delivery", "💳 Razorpay (UPI/Card)", "📅 Easy EMI"];

/** Next 7 selectable delivery dates (starting tomorrow). */
function dateOptions() {
  return Array.from({ length: 7 }, (_, i) => {
    const value = deliveryDateOption(i + 1);
    const label = new Date(value + "T00:00:00").toLocaleDateString("en-IN", {
      weekday: "short",
      day: "numeric",
      month: "short",
    });
    return { value, label };
  });
}
const MODES = ["COD", "RAZORPAY", "EMI"];

function loadRazorpay(): Promise<boolean> {
  return new Promise((resolve) => {
    if (typeof window !== "undefined" && (window as any).Razorpay) return resolve(true);
    const s = document.createElement("script");
    s.src = "https://checkout.razorpay.com/v1/checkout.js";
    s.onload = () => resolve(true);
    s.onerror = () => resolve(false);
    document.body.appendChild(s);
  });
}

export default function CheckoutPage() {
  const { lines, subtotal, count, clear } = useCart();
  const [pm, setPm] = useState(0);
  const [orderNo, setOrderNo] = useState<string | null>(null);
  const [placing, setPlacing] = useState(false);
  const [msg, setMsg] = useState("");
  const formRef = useRef<HTMLFormElement>(null);
  const [slotDate, setSlotDate] = useState(() => deliveryDateOption(1));
  const [slot, setSlot] = useState(DELIVERY_SLOTS[0]);
  const total = Math.max(0, subtotal - (count > 0 ? EXCHANGE_BONUS : 0));

  function readContact() {
    const fd = new FormData(formRef.current ?? undefined);
    return {
      name: String(fd.get("name") || ""),
      phone: String(fd.get("phone") || ""),
      email: String(fd.get("email") || ""),
      address: String(fd.get("address") || ""),
      city: String(fd.get("city") || ""),
      pincode: String(fd.get("pincode") || ""),
    };
  }

  function finish(no: string) {
    clear();
    setOrderNo(no);
    setPlacing(false);
  }

  function cartItems() {
    // Only ids + quantities — prices and totals are computed on the server.
    return lines.map(({ product: p, qty }) => ({ id: p.id, qty }));
  }

  async function persistOrder(mode: string, contact: any) {
    try {
      const res = await fetch("/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          items: cartItems(),
          contact,
          paymentMode: mode,
          delivery: { date: slotDate, slot },
        }),
      });
      const data = await res.json();
      if (res.ok && data.orderNo) {
        finish(data.orderNo);
      } else {
        setMsg(data.error || "Could not place the order — please try again.");
        setPlacing(false);
      }
    } catch {
      setMsg("Could not place the order — please check your connection and try again.");
      setPlacing(false);
    }
  }

  async function placeOrder() {
    if (count === 0 || placing) return;
    setMsg("");
    const mode = MODES[pm];
    const contact = readContact();

    const check = validateContact(contact);
    if (!check.ok) {
      setMsg(check.error);
      return;
    }
    const emailCheck = validateEmail(contact.email);
    if (!emailCheck.ok) {
      setMsg(emailCheck.error);
      return;
    }

    setPlacing(true);
    const orderPayload = { items: cartItems(), contact, delivery: { date: slotDate, slot } };

    if (mode === "RAZORPAY") {
      try {
        const ro = await fetch("/api/razorpay/order", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ items: cartItems() }),
        }).then((r) => r.json());

        if (ro?.error) {
          setMsg(ro.error);
          setPlacing(false);
          return;
        }
        if (ro?.orderId && (await loadRazorpay())) {
          const rzp = new (window as any).Razorpay({
            key: ro.keyId,
            amount: ro.amount,
            currency: "INR",
            name: "JL Enterprises",
            description: "Order payment",
            order_id: ro.orderId,
            prefill: { name: contact.name, contact: contact.phone },
            theme: { color: "#0b2447" },
            handler: async (resp: any) => {
              try {
                const v = await fetch("/api/razorpay/verify", {
                  method: "POST",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify({ ...resp, order: orderPayload }),
                }).then((r) => r.json());
                if (v.orderNo) finish(v.orderNo);
                else { setMsg(v.error || "Payment captured but verification failed — contact us."); setPlacing(false); }
              } catch {
                setPlacing(false);
              }
            },
            modal: { ondismiss: () => { setPlacing(false); setMsg("Payment cancelled."); } },
          });
          rzp.open();
          return; // wait for Razorpay handler
        }
        // No keys yet (demo) → fall through to demo order
        setMsg("Razorpay runs in demo mode until JL's keys are added.");
      } catch {
        setMsg("Could not start payment — placed as pending instead.");
      }
    }

    // COD / EMI / Razorpay-demo
    await persistOrder(mode, contact);
  }

  if (orderNo) {
    return (
      <div className="max-w-[1180px] mx-auto px-5 text-center py-20 animate-fade">
        <span className="text-6xl block mb-3">🎉</span>
        <h2 className="text-2xl text-navy font-bold mb-2">Order Placed Successfully!</h2>
        <p className="text-slate-600">
          Your order <b>#{orderNo}</b> is confirmed.<br />
          You'll get WhatsApp updates as it moves to delivery.
        </p>
        <div className="flex gap-3 justify-center mt-6 flex-wrap">
          <Link href="/" className="bg-navy text-white px-6 py-3 rounded-full font-semibold">Continue Shopping</Link>
          <Link href="/service" className="bg-teal-700 text-white px-6 py-3 rounded-full font-semibold">Book Installation</Link>
        </div>
      </div>
    );
  }

  if (count === 0) {
    return (
      <div className="max-w-[1180px] mx-auto px-5 text-center py-20 text-slate-400 animate-fade">
        Your cart is empty.
        <div className="mt-4">
          <Link href="/category" className="bg-navy text-white px-6 py-2.5 rounded-full font-semibold">Browse Products</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-[1180px] mx-auto px-5 animate-fade">
      <Link href="/cart" className="inline-block text-brand text-sm font-semibold my-4">← Back to cart</Link>
      <h2 className="text-2xl text-navy font-bold mb-4">Checkout</h2>

      <div className="grid md:grid-cols-[1fr_340px] gap-6 mb-8">
        <div>
          <form ref={formRef} className="bg-white border border-slate-200 rounded-2xl p-5 mb-4">
            <h3 className="text-navy font-semibold mb-4">📍 Delivery Address</h3>
            <Input label="Full Name" name="name" required minLength={2} maxLength={80} autoComplete="name" />
            <Input label="Phone" name="phone" placeholder="+91 " required inputMode="tel" autoComplete="tel" />
            <Input label="Email (optional — for order updates)" name="email" type="email" placeholder="you@example.com" autoComplete="email" />
            <div className="mb-3">
              <label className="text-[13px] text-slate-400 block mb-1.5">Address</label>
              <textarea name="address" rows={2} required minLength={10} maxLength={500} placeholder="Door no, street, area" className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm" />
            </div>
            <div className="flex gap-3">
              <Input label="City" name="city" placeholder="Thoothukudi" required minLength={2} maxLength={60} />
              <Input label="Pincode" name="pincode" placeholder="628008" required inputMode="numeric" pattern="[1-9][0-9]{5}" maxLength={6} />
              <Input label="State" name="state" defaultValue="Tamil Nadu" readOnly />
            </div>
          </form>

          <div className="bg-white border border-slate-200 rounded-2xl p-5 mb-4">
            <h3 className="text-navy font-semibold mb-4">🗓️ Preferred Delivery Slot</h3>
            <div className="mb-3">
              <label className="text-[13px] text-slate-400 block mb-1.5">Date</label>
              <div className="flex gap-2 flex-wrap">
                {dateOptions().map((d) => (
                  <button
                    key={d.value}
                    type="button"
                    onClick={() => setSlotDate(d.value)}
                    className={`border-2 rounded-xl px-3 py-2 text-[13px] font-semibold ${
                      slotDate === d.value ? "border-orange bg-orange-50 text-orange-600" : "border-slate-200 text-slate-600"
                    }`}
                  >
                    {d.label}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label className="text-[13px] text-slate-400 block mb-1.5">Time</label>
              <div className="flex gap-2 flex-wrap">
                {DELIVERY_SLOTS.map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => setSlot(s)}
                    className={`border-2 rounded-xl px-3 py-2 text-[13px] font-semibold ${
                      slot === s ? "border-orange bg-orange-50 text-orange-600" : "border-slate-200 text-slate-600"
                    }`}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
            <p className="text-[12px] text-slate-400 mt-2">Our team confirms the exact slot on WhatsApp before dispatch.</p>
          </div>

          <div className="bg-white border border-slate-200 rounded-2xl p-5">
            <h3 className="text-navy font-semibold mb-4">💳 Payment Method</h3>
            <div className="flex gap-3 flex-wrap">
              {PAYMENTS.map((label, i) => (
                <button
                  key={label}
                  onClick={() => setPm(i)}
                  className={`flex-1 min-w-[120px] border-2 rounded-xl p-3.5 text-sm font-semibold ${
                    pm === i ? "border-orange bg-orange-50 text-orange-600" : "border-slate-200 text-slate-600"
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
            {pm === 1 && (
              <p className="text-[12px] text-slate-400 mt-2">
                🔒 Secure payment via Razorpay (UPI, cards, net-banking). Settles to JL's bank.
              </p>
            )}
          </div>
        </div>

        <div className="bg-white border border-slate-200 rounded-2xl p-5 h-fit">
          <h3 className="text-navy font-semibold mb-4">Order Summary</h3>
          {lines.map(({ product: p, qty }) => (
            <div key={p.id} className="flex justify-between py-1.5 text-[13px] text-slate-600">
              <span>{p.name.split(" ").slice(0, 3).join(" ")} ×{qty}</span>
              <span>{inr(p.price * qty)}</span>
            </div>
          ))}
          <div className="flex justify-between py-1.5 text-sm text-slate-600 border-t border-slate-100 mt-1"><span>Delivery</span><span className="text-green-600">FREE</span></div>
          <div className="flex justify-between py-1.5 text-sm text-slate-600"><span>Exchange bonus</span><span className="text-green-600">- {inr(EXCHANGE_BONUS)}</span></div>
          <div className="flex justify-between border-t border-slate-200 mt-2 pt-3.5 text-lg font-extrabold text-navy"><span>Total</span><span>{inr(total)}</span></div>
          {msg && <p className="text-[12px] text-orange-600 mt-2">{msg}</p>}
          <button onClick={placeOrder} disabled={placing} className="w-full bg-gradient-to-br from-orange to-amber disabled:opacity-60 text-white font-bold py-3.5 rounded-full mt-4">
            {placing ? "Processing…" : pm === 1 ? "Pay Now 💳" : "Place Order ✅"}
          </button>
        </div>
      </div>
    </div>
  );
}

function Input({ label, ...rest }: { label: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <div className="mb-3 flex-1">
      <label className="text-[13px] text-slate-400 block mb-1.5">{label}</label>
      <input {...rest} className="w-full border border-slate-200 rounded-lg px-3 py-2.5 text-sm" />
    </div>
  );
}
