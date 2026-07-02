import { ADDRESS, CONTACTS, prettyPhone } from "@/lib/contact";

export const metadata = { title: "Shipping & Delivery Policy — JL Enterprises" };

export default function ShippingPolicy() {
  return (
    <>
      <h1>Shipping &amp; Delivery Policy</h1>
      <p className="updated">Last updated: 2 July 2026</p>

      <h2>1. Delivery area</h2>
      <p>
        We deliver from our showroom at {ADDRESS}, serving <strong>Thoothukudi city and surrounding
        areas of Thoothukudi district</strong>. If your pincode is outside our service area, our
        team will call you after the order to confirm feasibility or arrange a refund.
      </p>

      <h2>2. Delivery charges &amp; timelines</h2>
      <ul>
        <li>Delivery is <strong>FREE</strong> for all orders placed on this store.</li>
        <li>In-stock items within Thoothukudi city are typically delivered in <strong>1–3 working days</strong>.</li>
        <li>Surrounding areas and items sourced from the brand&rsquo;s warehouse may take <strong>3–7 working days</strong>.</li>
        <li>Our team confirms a delivery slot with you on phone/WhatsApp before dispatch.</li>
      </ul>

      <h2>3. Installation</h2>
      <ul>
        <li>Products marked with free installation (e.g. air conditioners) are installed by our in-house team or the brand&rsquo;s authorised technicians after delivery.</li>
        <li>Installation is usually scheduled within 48 hours of delivery.</li>
      </ul>

      <h2>4. Order tracking</h2>
      <p>
        You will receive WhatsApp updates on the number given at checkout as your order moves from
        confirmation to dispatch to delivery. You can also call{" "}
        <strong>{prettyPhone(CONTACTS.sales.phone)}</strong> ({CONTACTS.sales.label}) with your
        order number at any time.
      </p>

      <h2>5. At delivery</h2>
      <ul>
        <li>Please inspect the product at delivery and note any external damage with our delivery staff.</li>
        <li>For Cash on Delivery orders, payment is collected in full at handover.</li>
      </ul>
    </>
  );
}
