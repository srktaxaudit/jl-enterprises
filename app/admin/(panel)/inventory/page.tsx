import { adminProducts } from "@/lib/admin-data";
import { PageHead } from "@/components/admin/ui";
import InventoryTable from "@/components/admin/InventoryTable";

export default async function InventoryPage() {
  const products = await adminProducts();
  return (
    <div>
      <PageHead title="🗂️ Inventory" sub="Live stock — adjust levels; every sale auto-decrements." />
      <InventoryTable products={products} />
    </div>
  );
}
