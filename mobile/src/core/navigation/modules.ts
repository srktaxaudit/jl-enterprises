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
  { key: "whatsapp", label: "WhatsApp Offers", icon: "💬", route: "/(app)/whatsapp", rule: "MARKETING_MANAGER", group: "Engage", built: false },
  { key: "billing", label: "Billing", icon: "🧾", route: "/(app)/billing", rule: "@admin", group: "Accounting", built: false },
  { key: "accounts", label: "Chart of Accounts", icon: "📒", route: "/(app)/accounts", rule: "ACCOUNTANT", group: "Accounting", built: false },
  { key: "vouchers", label: "Invoices & Bills", icon: "🧾", route: "/(app)/vouchers", rule: "ACCOUNTANT", group: "Accounting", built: false },
  { key: "journal", label: "Journal / Vouchers", icon: "✍️", route: "/(app)/journal", rule: "ACCOUNTANT", group: "Accounting", built: false },
  { key: "ledgers", label: "Ledgers", icon: "📚", route: "/(app)/ledgers", rule: "ACCOUNTANT", group: "Accounting", built: false },
  { key: "reports", label: "Financial Reports", icon: "📈", route: "/(app)/reports", rule: "ACCOUNTANT", group: "Accounting", built: false },
  { key: "gst", label: "GST Returns", icon: "🧮", route: "/(app)/gst", rule: "ACCOUNTANT", group: "Accounting", built: false },
  { key: "outstanding", label: "Outstanding & Cashflow", icon: "📆", route: "/(app)/outstanding", rule: "ACCOUNTANT", group: "Accounting", built: false },
  { key: "staff", label: "Staff", icon: "🧑‍💼", route: "/(app)/staff", rule: "@admin", group: "Control", built: false },
  { key: "team", label: "Team & Roles", icon: "👤", route: "/(app)/team", rule: "@admin", group: "Control", built: false },
  { key: "logs", label: "Activity Logs", icon: "📜", route: "/(app)/logs", rule: "@admin", group: "Control", built: false },
  { key: "data", label: "Import / Export", icon: "🔄", route: "/(app)/data", rule: "ACCOUNTANT", group: "Control", built: false },
  { key: "branding", label: "Logo & Branding", icon: "🖼️", route: "/(app)/branding", rule: "@admin", group: "Control", built: false },
  { key: "settings", label: "Settings", icon: "⚙️", route: "/(app)/settings", rule: "@admin", group: "Control", built: false },
];

export const GROUP_ORDER: AdminModule["group"][] = [
  "Overview",
  "Catalog & Sales",
  "Engage",
  "Accounting",
  "Control",
];
