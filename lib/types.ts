export type Category = {
  id: string;
  slug: string;
  name: string;
  emoji: string;
};

export type Product = {
  id: string;
  slug: string;
  name: string;
  brand: string;
  categorySlug: string;
  emoji: string;
  imageUrl?: string | null;
  description?: string;
  specs?: Record<string, string>;
  price: number;
  mrp: number;
  stock: number;
  rating: number;
  reviewCount: number;
  emiPerMonth: number;
  isActive: boolean;
  isFeatured?: boolean;
};

export type CartLine = {
  product: Product;
  qty: number;
};
