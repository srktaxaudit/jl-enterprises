import { getCategories } from "@/lib/catalog";
import { PageHead } from "@/components/admin/ui";
import ProductForm from "@/components/admin/ProductForm";

export default function NewProductPage() {
  const categories = getCategories().map((c) => ({ slug: c.slug, name: c.name }));
  return (
    <div>
      <PageHead title="➕ Add Product" sub="Create a new product for the storefront." />
      <ProductForm categories={categories} />
    </div>
  );
}
