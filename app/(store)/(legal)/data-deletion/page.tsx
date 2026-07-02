import Link from "next/link";
import { CONTACTS, prettyPhone, waHref } from "@/lib/contact";

export const metadata = { title: "Data Deletion — JL Enterprises" };

export default function DataDeletion() {
  return (
    <>
      <h1>Data Deletion Requests</h1>
      <p className="updated">Last updated: 2 July 2026</p>

      <p>
        You can ask JL Enterprises to delete the personal data we hold about you — your name, phone
        number, delivery addresses, email and account details — at any time. This page describes
        the process for both the website and the JL Enterprises mobile app.
      </p>

      <h2>1. How to request deletion</h2>
      <ul>
        <li>
          <strong>WhatsApp / phone:</strong> message or call{" "}
          <a href={waHref(CONTACTS.owner.phone, "Please delete my personal data from JL Store.")}>
            {prettyPhone(CONTACTS.owner.phone)}
          </a>{" "}
          ({CONTACTS.owner.label}) from the mobile number you used on the store, with the message
          &ldquo;Please delete my personal data&rdquo;.
        </li>
        <li>
          <strong>In person:</strong> visit our showroom with the phone used for your orders.
        </li>
      </ul>
      <p>
        We verify the request using the mobile number on the order (an OTP or call-back), so nobody
        else can delete your data.
      </p>

      <h2>2. What gets deleted</h2>
      <ul>
        <li>Your account (email login) and saved contact details.</li>
        <li>Your name, phone number and address attached to past orders.</li>
        <li>Your number is removed from WhatsApp updates and broadcasts.</li>
      </ul>

      <h2>3. What we must keep (and for how long)</h2>
      <p>
        Tax law requires a retailer to preserve invoices and books of account. For orders you
        placed, the financial record (order number, items, amounts, GST details) is retained for up
        to 8 years as required by GST and income-tax law, but it is unlinked from your deleted
        contact details wherever possible and never used for marketing.
      </p>

      <h2>4. Timeline</h2>
      <p>
        Deletion is completed within <strong>30 days</strong> of a verified request, and we confirm
        completion on the same channel you used to ask.
      </p>

      <p>
        See our <Link href="/privacy">Privacy Policy</Link> for full details of what we collect and
        why.
      </p>
    </>
  );
}
