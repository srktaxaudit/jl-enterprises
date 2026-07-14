/** Mirrors the backend response envelopes (in.jlenterprises.ecommerce.response). */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// ── Auth ──

export interface AuthUser {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  whatsappOptIn?: boolean;
  roles: string[];
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

// ── Catalog ──

export interface ProductSummary {
  id: string;
  name: string;
  slug: string;
  sku: string;
  price: number;
  comparePrice?: number | null;
  discountPercent?: number | null;
  currency: string;
  featured: boolean;
  averageRating?: number | null;
  reviewCount: number;
  primaryImageUrl?: string | null;
  brandName?: string | null;
  categorySlug?: string | null;
  availableStock?: number | null;
  emiAvailable: boolean;
  emiAmount?: number | null;
  emiMonths?: number | null;
}

export interface ProductImage {
  id: string;
  url: string;
  altText?: string | null;
  primary?: boolean;
  sortOrder?: number;
}

export interface ProductVariant {
  id: string;
  name?: string | null;
  sku?: string | null;
  price?: number | null;
  availableStock?: number | null;
}

export interface ProductDetail extends Omit<ProductSummary, "brandName" | "categorySlug"> {
  shortDescription?: string | null;
  description?: string | null;
  specifications?: string | null;
  viewCount: number;
  salesCount: number;
  categoryId?: string | null;
  categorySlug?: string | null;
  brandId?: string | null;
  brandName?: string | null;
  images: ProductImage[];
  variants: ProductVariant[];
  emiDownPayment?: number | null;
  emiProcessingFee?: number | null;
  emiNote?: string | null;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  description?: string | null;
  imageUrl?: string | null;
  sortOrder: number;
  parentId?: string | null;
}

export interface Brand {
  id: string;
  name: string;
  slug: string;
  logoUrl?: string | null;
  description?: string | null;
}

export interface Banner {
  id: string;
  title?: string | null;
  imageUrl: string;
  linkUrl?: string | null;
  position?: string | null;
  sortOrder: number;
  active: boolean;
}

// ── Reviews ──

export interface Review {
  id: string;
  productId: string;
  reviewerName?: string | null;
  rating: number;
  title?: string | null;
  comment?: string | null;
  status: string;
  verifiedPurchase: boolean;
  createdAt: string;
}

// ── Cart / wishlist ──

export interface CartItem {
  id: string;
  productId: string;
  productName: string;
  slug: string;
  primaryImageUrl?: string | null;
  variantId?: string | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Cart {
  id: string;
  items: CartItem[];
  itemCount: number;
  subtotal: number;
}

export interface WishlistItem {
  id: string;
  product: ProductSummary;
}

export interface Wishlist {
  id: string;
  items: WishlistItem[];
}

// ── Coupons ──

export interface Coupon {
  id: string;
  code: string;
  description?: string | null;
  discountType?: string | null;
  discountValue?: number | null;
  minOrderAmount?: number | null;
  expiresAt?: string | null;
}

export interface CouponValidationResult {
  valid: boolean;
  message?: string | null;
  code?: string | null;
  discountAmount?: number | null;
}

// ── Addresses ──

export type AddressType = "SHIPPING" | "BILLING" | "BOTH";

export interface Address {
  id: string;
  type?: AddressType | null;
  fullName?: string | null;
  phone?: string | null;
  line1: string;
  line2?: string | null;
  city: string;
  state?: string | null;
  postalCode: string;
  country?: string | null;
  defaultAddress: boolean;
}

export interface AddressInput {
  type?: AddressType;
  fullName?: string;
  phone?: string;
  line1: string;
  line2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country?: string;
  defaultAddress?: boolean;
}

// ── Orders ──

export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PROCESSING"
  | "PACKED"
  | "SHIPPED"
  | "OUT_FOR_DELIVERY"
  | "DELIVERED"
  | "CANCELLED"
  | "RETURN_REQUESTED"
  | "RETURNED"
  | "REFUNDED"
  | "FAILED_PAYMENT";

export type PaymentMethod = "COD" | "RAZORPAY" | "STRIPE" | "PAYPAL";

export interface OrderItem {
  id: string;
  productId?: string | null;
  productName: string;
  slug?: string | null;
  imageUrl?: string | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface AddressSnapshot {
  fullName?: string | null;
  phone?: string | null;
  line1?: string | null;
  line2?: string | null;
  city?: string | null;
  state?: string | null;
  postalCode?: string | null;
  country?: string | null;
}

export interface OrderPayment {
  method?: PaymentMethod | string | null;
  status?: string | null;
  transactionId?: string | null;
}

export interface Order {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  subtotal: number;
  discountTotal: number;
  taxTotal: number;
  shippingTotal: number;
  grandTotal: number;
  currency: string;
  couponCode?: string | null;
  shippingAddress?: AddressSnapshot | null;
  billingAddress?: AddressSnapshot | null;
  notes?: string | null;
  placedAt: string;
  cancelledAt?: string | null;
  cancellationReason?: string | null;
  returnReason?: string | null;
  items: OrderItem[];
  payment?: OrderPayment | null;
}

export interface OrderSummary {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  grandTotal: number;
  currency: string;
  itemCount: number;
  placedAt: string;
}

export interface OrderTracking {
  orderNumber: string;
  status: OrderStatus;
  placedAt: string;
}
