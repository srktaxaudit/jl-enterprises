import { CONTACTS, prettyPhone } from "@/lib/contact";

export const metadata = { title: "Refund & Cancellation Policy — JL Enterprises" };

export default function RefundPolicy() {
  return (
    <>
      <h1>Refund &amp; Cancellation Policy</h1>
      <p className="updated">Last updated: 2 July 2026</p>

      <h2>1. Cancelling an order</h2>
      <ul>
        <li>You can cancel an order free of charge any time <strong>before it is dispatched</strong> by calling or WhatsApp-ing us at {prettyPhone(CONTACTS.sales.phone)}.</li>
        <li>Prepaid amounts for cancelled orders are refunded in full.</li>
        <li>Once an appliance has been dispatched or installed, cancellation is treated as a return request (below).</li>
      </ul>

      <h2>2. Returns &amp; replacements</h2>
      <ul>
        <li><strong>Damaged on delivery / dead on arrival</strong> — report within <strong>48 hours</strong> of delivery with photos; we will replace the unit or refund in full at your choice.</li>
        <li><strong>Wrong item delivered</strong> — we will collect it and deliver the correct item at no cost.</li>
        <li><strong>Manufacturing defects after use</strong> — covered by the manufacturer&rsquo;s warranty; we will help you register the claim with the brand&rsquo;s authorised service centre.</li>
        <li>Items must be unused (except to discover the defect) and returned with original packaging and accessories where possible.</li>
        <li>Change-of-mind returns on opened or installed appliances are not accepted, in line with industry practice for large appliances.</li>
      </ul>

      <h2>3. Refund timelines</h2>
      <ul>
        <li><strong>Razorpay payments</strong> (UPI/card/net-banking) — refunded to the original payment method within <strong>5–7 business days</strong> of the return being approved.</li>
        <li><strong>Cash on Delivery</strong> — refunded by bank transfer within 7 business days of the return being approved (we will collect your bank details securely).</li>
        <li>The exchange bonus and any discounts are adjusted proportionately in the refund.</li>
      </ul>

      <h2>4. How to raise a request</h2>
      <p>
        Call or WhatsApp <strong>{prettyPhone(CONTACTS.sales.phone)}</strong> ({CONTACTS.sales.label})
        or <strong>{prettyPhone(CONTACTS.service.phone)}</strong> ({CONTACTS.service.label}) with your
        order number (e.g. JL123456). We acknowledge every request within 1 working day.
      </p>
    </>
  );
}
