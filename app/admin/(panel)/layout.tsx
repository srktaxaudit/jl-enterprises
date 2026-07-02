import { redirect } from "next/navigation";
import { isAdmin } from "@/lib/admin-auth";
import AdminShell from "@/components/admin/AdminShell";

export const metadata = { title: "JL Enterprises — Admin" };

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  // Middleware only checks cookie presence (edge runtime); the signed
  // session is verified here, where Node crypto is available.
  if (!isAdmin()) redirect("/admin/login");
  return <AdminShell>{children}</AdminShell>;
}
