// Central contact directory for JL Enterprises — reused across the
// storefront (footer, WhatsApp, service) and the admin panel.

export type Contact = { label: string; phone: string };

export const CONTACTS: Record<string, Contact> = {
  sales: { label: "Sales Team", phone: "9514970111" },
  marketing: { label: "Marketing Team", phone: "9514970222" },
  service: { label: "Service Team", phone: "9514970444" },
  owner: { label: "Owner / Enquiries", phone: "7373970666" },
};

export const CONTACT_LIST = Object.values(CONTACTS);

/** Number used for the storefront WhatsApp chat button (customer enquiries). */
export const PRIMARY_WHATSAPP = CONTACTS.sales.phone;

export const ADDRESS =
  "185G/1B, Palai Road, Chidambaramnagar, Thoothukudi, Tamil Nadu 628008";

export function telHref(phone: string) {
  return "tel:+91" + phone.replace(/\D/g, "");
}

export function waHref(phone: string, text?: string) {
  const base = "https://wa.me/91" + phone.replace(/\D/g, "");
  return text ? `${base}?text=${encodeURIComponent(text)}` : base;
}

export function prettyPhone(phone: string) {
  const d = phone.replace(/\D/g, "");
  return d.length === 10 ? `${d.slice(0, 5)} ${d.slice(5)}` : phone;
}
