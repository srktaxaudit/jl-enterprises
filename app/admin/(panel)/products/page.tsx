import { adminProducts } from "@/lib/admin-data";
import { PageHead } from "@/components/admin/ui";
import ProductsTable from "@/components/admin/ProductsTable";

export default async function ProductsPage() {
  const products = await adminProducts();
  return (
    <div>
      <PageHead title="📦 Products" sub="Create, edit and enable/disable products." />
      <ProductsTable products={products} />
    </div>
  );
}
