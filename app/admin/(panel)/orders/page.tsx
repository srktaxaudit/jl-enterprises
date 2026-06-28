import { adminOrders } from "@/lib/admin-data";
import { PageHead } from "@/components/admin/ui";
import OrdersTable from "@/components/admin/OrdersTable";

export default async function OrdersPage() {
  const orders = await adminOrders();
  return (
    <div>
      <PageHead title="📥 Orders" sub="Receive, process and dispatch orders — status changes notify the customer on WhatsApp." />
      <OrdersTable orders={orders} />
    </div>
  );
}
