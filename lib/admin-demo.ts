// Demo orders & customers shown in the admin panel when Supabase isn't
// connected — so the CEO view looks alive during development/preview.

export type AdminOrder = {
  id: string;
  order_no: string;
  contact_name: string;
  city: string;
  product: string;
  payment_mode: string;
  status: string;
  total: number;
};

export const DEMO_ORDERS: AdminOrder[] = [
  { id: "o1", order_no: "JL2841", contact_name: "Ramesh K.", city: "Tuticorin", product: "Voltas 1.5T AC", payment_mode: "COD", status: "OUT_FOR_DELIVERY", total: 34990 },
  { id: "o2", order_no: "JL2840", contact_name: "Priya S.", city: "Tirunelveli", product: "LG Washing Machine", payment_mode: "RAZORPAY", status: "PACKED", total: 28500 },
  { id: "o3", order_no: "JL2839", contact_name: "Karthik M.", city: "Kovilpatti", product: 'Sony 55" 4K TV', payment_mode: "COD", status: "NEW", total: 62990 },
  { id: "o4", order_no: "JL2838", contact_name: "Anitha R.", city: "Tuticorin", product: "Preethi Grinder + Stove", payment_mode: "UPI", status: "DELIVERED", total: 9450 },
  { id: "o5", order_no: "JL2837", contact_name: "Suresh B.", city: "Madurai", product: "Godrej Fridge", payment_mode: "EMI", status: "OUT_FOR_DELIVERY", total: 41200 },
  { id: "o6", order_no: "JL2836", contact_name: "Deepa V.", city: "Tuticorin", product: "Sofa Set (5-seater)", payment_mode: "RAZORPAY", status: "NEW", total: 38000 },
];

export type AdminCustomer = {
  id: string;
  name: string;
  phone: string;
  area: string;
  orders: number;
  ltv: number;
};

export const DEMO_CUSTOMERS: AdminCustomer[] = [
  { id: "c1", name: "Ramesh Kumar", phone: "+91 98xxxx2210", area: "Tuticorin", orders: 6, ltv: 184000 },
  { id: "c2", name: "Priya S.", phone: "+91 99xxxx4471", area: "Tirunelveli", orders: 3, ltv: 78000 },
  { id: "c3", name: "Karthik M.", phone: "+91 90xxxx1182", area: "Kovilpatti", orders: 2, ltv: 91000 },
  { id: "c4", name: "Anitha R.", phone: "+91 94xxxx7765", area: "Tuticorin", orders: 5, ltv: 112000 },
  { id: "c5", name: "Suresh B.", phone: "+91 73xxxx5540", area: "Madurai", orders: 1, ltv: 41000 },
];

export const STATUS_FLOW = ["NEW", "PACKED", "OUT_FOR_DELIVERY", "DELIVERED"];

export const STATUS_LABEL: Record<string, string> = {
  NEW: "New",
  PACKED: "Packed",
  OUT_FOR_DELIVERY: "Out for delivery",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};
