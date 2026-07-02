import Link from "next/link";
import { ADDRESS, CONTACTS, prettyPhone } from "@/lib/contact";

export const metadata = { title: "Terms & Conditions — JL Enterprises" };

export default function Terms() {
  return (
    <>
      <h1>Terms &amp; Conditions</h1>
      <p className="updated">Last updated: 2 July 2026</p>

      <p>
        These terms govern purchases made from JL Enterprises, {ADDRESS}, through this website and
        mobile app. By placing an order you agree to these terms.
      </p>

      <h2>1. Products &amp; pricing</h2>
      <ul>
        <li>All prices are in Indian Rupees (₹) and include applicable GST unless stated otherwise.</li>
        <li>MRP strike-through prices are the manufacturer&rsquo;s listed price; our selling price is what you pay.</li>
        <li>Prices and offers (including the old-appliance exchange bonus) may change without notice; the price at the time your order is confirmed is final.</li>
        <li>Product images and specifications are indicative; minor variations from the manufacturer are possible.</li>
      </ul>

      <h2>2. Orders</h2>
      <ul>
        <li>An order is confirmed when you receive an order number on screen and a WhatsApp confirmation.</li>
        <li>We may cancel an order if a product is out of stock, mispriced due to an obvious error, or if delivery to your address isn&rsquo;t serviceable — anything paid is refunded in full.</li>
        <li>Order amounts are computed on our servers from the current catalog price; the amount charged always matches the confirmed order.</li>
      </ul>

      <h2>3. Payment</h2>
      <ul>
        <li><strong>Online payment</strong> — processed securely by Razorpay (UPI, cards, net-banking). We never store your payment credentials.</li>
        <li><strong>Cash on Delivery</strong> — pay in full at the time of delivery.</li>
        <li><strong>EMI</strong> — subject to eligibility and confirmation by our team after the order is placed.</li>
      </ul>

      <h2>4. Delivery, returns &amp; refunds</h2>
      <p>
        Delivery timelines and serviceable areas are described in the{" "}
        <Link href="/shipping-policy">Shipping &amp; Delivery Policy</Link>. Returns, replacements
        and refunds are governed by the <Link href="/refund-policy">Refund &amp; Cancellation Policy</Link>.
      </p>

      <h2>5. Warranty &amp; service</h2>
      <ul>
        <li>Products carry the manufacturer&rsquo;s warranty as stated on the product page. Warranty claims are honoured through the brand&rsquo;s authorised service network.</li>
        <li>Installation and after-sales service booked through us is performed by our in-house team or authorised partners.</li>
      </ul>

      <h2>6. Acceptable use</h2>
      <p>
        You agree not to misuse this site — including placing fraudulent orders, tampering with
        prices or requests, or attempting to access other customers&rsquo; data or the admin panel.
      </p>

      <h2>7. Liability</h2>
      <p>
        Our liability for any claim relating to an order is limited to the amount paid for that
        order. Nothing in these terms limits rights you have under the Consumer Protection Act, 2019.
      </p>

      <h2>8. Governing law</h2>
      <p>
        These terms are governed by Indian law. Disputes are subject to the jurisdiction of the
        courts at Thoothukudi, Tamil Nadu.
      </p>

      <h2>9. Contact</h2>
      <p>
        JL Enterprises, {ADDRESS}. Phone: <strong>{prettyPhone(CONTACTS.owner.phone)}</strong>{" "}
        ({CONTACTS.owner.label}) · <strong>{prettyPhone(CONTACTS.sales.phone)}</strong> ({CONTACTS.sales.label}).
      </p>
    </>
  );
}
