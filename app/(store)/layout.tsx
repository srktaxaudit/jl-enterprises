import { CartProvider } from "@/lib/cart";
import { WishlistProvider } from "@/lib/wishlist";
import { fetchCategories } from "@/lib/data";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import FloatingCart from "@/components/FloatingCart";

export default async function StoreLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  // Live categories (Supabase, seed fallback) so the header nav always
  // links to category pages that actually exist.
  const cats = await fetchCategories();

  return (
    <CartProvider>
      <WishlistProvider>
        <Header cats={cats} />
        <main className="min-h-[60vh]">{children}</main>
        <Footer />
        <FloatingCart />
      </WishlistProvider>
    </CartProvider>
  );
}
