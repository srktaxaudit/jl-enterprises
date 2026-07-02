import Link from "next/link";
import { ADDRESS, CONTACTS, prettyPhone } from "@/lib/contact";

export const metadata = { title: "Privacy Policy — JL Enterprises" };

export default function PrivacyPolicy() {
  return (
    <>
      <h1>Privacy Policy</h1>
      <p className="updated">Last updated: 2 July 2026</p>

      <p>
        JL Enterprises (&ldquo;we&rdquo;, &ldquo;us&rdquo;, &ldquo;our&rdquo;), {ADDRESS}, operates this
        online store and mobile app for home appliances, electronics and furniture. This policy
        explains what personal data we collect, why we collect it, and the rights you have over it,
        in line with the Digital Personal Data Protection Act, 2023 (DPDP Act) and other applicable
        Indian law.
      </p>

      <h2>1. Data we collect</h2>
      <ul>
        <li><strong>Order &amp; delivery details</strong> — your name, mobile number, delivery address, city and pincode, collected when you place an order or book a service visit.</li>
        <li><strong>Order history</strong> — the products you purchased, amounts paid and payment method.</li>
        <li><strong>Account data</strong> — your email address, if you sign in with email OTP.</li>
        <li><strong>Payment data</strong> — payments are processed by Razorpay. We never see or store your card number, UPI PIN or banking credentials; we only receive a payment reference and status from Razorpay.</li>
      </ul>
      <p>We do not collect data we don&rsquo;t need — there is no advertising tracking or sale of your data to third parties.</p>

      <h2>2. Why we use it</h2>
      <ul>
        <li>To process, deliver and provide after-sales support for your orders.</li>
        <li>To send order confirmations and delivery updates on WhatsApp to the number you provide.</li>
        <li>To arrange installation or service visits you request.</li>
        <li>To maintain the accounts, invoices and records that tax law requires a retailer to keep.</li>
      </ul>

      <h2>3. Who we share it with</h2>
      <ul>
        <li><strong>Razorpay</strong> — to process online payments (see the <a href="https://razorpay.com/privacy/" target="_blank" rel="noopener noreferrer">Razorpay privacy policy</a>).</li>
        <li><strong>Meta (WhatsApp Business)</strong> — your phone number and order status, to deliver order updates.</li>
        <li><strong>Supabase</strong> — our database and hosting provider, which stores order data on our behalf.</li>
        <li><strong>Delivery and installation staff</strong> — your name, address and phone number, only for fulfilling your order.</li>
      </ul>
      <p>We do not sell or rent personal data to anyone.</p>

      <h2>4. How long we keep it</h2>
      <p>
        Order and invoice records are retained for as long as GST and income-tax law requires
        (generally up to 8 years), after which they are deleted. Data not needed for legal
        record-keeping is deleted on request — see our{" "}
        <Link href="/data-deletion">Data Deletion policy</Link>.
      </p>

      <h2>5. Your rights</h2>
      <ul>
        <li>Ask what personal data we hold about you.</li>
        <li>Ask us to correct inaccurate data.</li>
        <li>Ask us to delete your data (subject to the legal retention above).</li>
        <li>Raise a grievance about how your data is handled.</li>
      </ul>

      <h2>6. Security</h2>
      <p>
        Data is stored in an access-controlled database with row-level security, and payment
        verification happens only on our servers. Access to customer data inside JL Enterprises is
        limited to staff who need it to fulfil orders.
      </p>

      <h2>7. Contact / Grievance</h2>
      <p>
        For any privacy question, correction or complaint, contact us at the store address above or
        call <strong>{prettyPhone(CONTACTS.owner.phone)}</strong> ({CONTACTS.owner.label}). We aim
        to respond within 7 working days.
      </p>
    </>
  );
}
