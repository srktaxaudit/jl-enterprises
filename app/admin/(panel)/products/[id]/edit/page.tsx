import { notFound } from "next/navigation";
import { adminProducts } from "@/lib/admin-data";
import { fetchCategories } from "@/lib/data";
import { PageHead } from "@/components/admin/ui";
import ProductForm from "@/components/admin/ProductForm";

export default async function EditProductPage({ params }: { params: { id: string } }) {
  const products = await adminProducts();
  const product = products.find((p) => p.id === params.id);
  if (!product) notFound();
  const categories = (await fetchCategories()).map((c) => ({ slug: c.slug, name: c.name }));
  return (
    <div>
      <PageHead title="✏️ Edit Product" sub={product.name} />
      <ProductForm initial={product} categories={categories} />
    </div>
  );
}
