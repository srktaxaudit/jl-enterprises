/**
 * The full admin module registry — mirrors the web sidebar (admin-shell.js),
 * including the same RBAC rules so both clients show identical menus.
 *
 * `rule`: undefined = all staff · "@admin" = super/admin only ·
 * "A,B" = super/admin OR MANAGER OR any listed bare role.
 *
 * `route` is the expo-router path under /(app). Modules marked `built: false`
 * are scaffolded stubs that follow the Orders/Products reference pattern.
 */
export interface AdminModule {
  key: string;
  label: string;
  icon: string; // emoji placeholder; swap for @expo/vector-icons in production
  route: string;
  rule?: string;
  group: "Overview" | "Catalog & Sales" | "Engage" | "Accounting" | "Control";
  built: boolean;
}

export const MODULES: AdminModule[] = [
  { key: "dashboard", label: "Dashboard", icon: "📊", route: "/(app)/", group: "Overview", built: true },
  { key: "orders", label: "Orders", icon: "📥", route: "/(app)/orders", rule: "ORDER_MANAGER,CUSTOMER_SUPPORT", group: "Overview", built: true },
  { key: "products", label: "Products", icon: "📦", route: "/(app)/products", rule: "PRODUCT_MANAGER", group: "Catalog & Sales", built: true },
  { key: "inventory", label: "Inventory", icon: "🗂️", route: "/inventory", rule: "INVENTORY_MANAGER", group: "Catalog & Sales", built: true },
  { key: "offers", label: "Offers & Deals", icon: "🏷️", route: "/offers", rule: "MARKETING_MANAGER", group: "Catalog & Sales", built: true },
  { key: "customers", label: "Customers (CRM)", icon: "👥", route: "/customers", rule: "MANAGER", group: "Engage", built: true },
  { key: "reviews", label: "Reviews", icon: "⭐", route: "/reviews", rule: "MARKETING_MANAGER,CUSTOMER_SUPPORT", group: "Engage", built: true },
  { key: "service", label: "Service Bookings", icon: "🔧", route: "/service", rule: "CUSTOMER_SUPPORT", group: "Engage", built: true },
  { key: "exchange", label: "Exchange Requests", icon: "♻️", route: "/exchange", rule: "CUSTOMER_SUPPORT,MANAGER", group: "Engage", built: true },
  { key: "whatsapp", label: "WhatsApp Offers", icon: "💬", route: "/whatsapp", rule: "MARKETING_MANAGER", group: "Engage", built: true },
  { key: "billing", label: "Billing", icon: "🧾", route: "/billing", rule: "@admin", group: "Accounting", built: true },
  { key: "accounts", label: "Chart of Accounts", icon: "📒", route: "/accounts", rule: "ACCOUNTANT", group: "Accounting", built: true },
  { key: "vouchers", label: "Invoices & Bills", icon: "🧾", route: "/vouchers", rule: "ACCOUNTANT", group: "Accounting", built: true },
  { key: "journal", label: "Journal / Vouchers", icon: "✍️", route: "/journal", rule: "ACCOUNTANT", group: "Accounting", built: true },
  { key: "ledgers", label: "Ledgers", icon: "📚", route: "/ledgers", rule: "ACCOUNTANT", group: "Accounting", built: true },
  { key: "reports", label: "Financial Reports", icon: "📈", route: "/reports", rule: "ACCOUNTANT", group: "Accounting", built: true },
  { key: "gst", label: "GST Returns", icon: "🧮", route: "/gst", rule: "ACCOUNTANT", group: "Accounting", built: true },
  { key: "outstanding", label: "Outstanding & Cashflow", icon: "📆", route: "/outstanding", rule: "ACCOUNTANT", group: "Accounting", built: true },
  { key: "staff", label: "Staff", icon: "🧑‍💼", route: "/staff", rule: "@admin", group: "Control", built: true },
  { key: "team", label: "Team & Roles", icon: "👤", route: "/team", rule: "@admin", group: "Control", built: true },
  { key: "logs", label: "Activity Logs", icon: "📜", route: "/logs", rule: "@admin", group: "Control", built: true },
  { key: "data", label: "Import / Export", icon: "🔄", route: "/data", rule: "ACCOUNTANT", group: "Control", built: true },
  { key: "branding", label: "Logo & Branding", icon: "🖼️", route: "/branding", rule: "@admin", group: "Control", built: true },
  { key: "settings", label: "Settings", icon: "⚙️", route: "/settings", rule: "@admin", group: "Control", built: true },
];

export const GROUP_ORDER: AdminModule["group"][] = [
  "Overview",
  "Catalog & Sales",
  "Engage",
  "Accounting",
  "Control",
];
