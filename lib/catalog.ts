import type { Category, Product } from "./types";

// ════════════════════════════════════════════════════════════════════
//  Seed catalog — lets the store run BEFORE Supabase is connected.
//  Once NEXT_PUBLIC_SUPABASE_URL is set, swap these getters for the
//  Supabase queries in lib/data.server.ts (Phase 2 wiring).
// ════════════════════════════════════════════════════════════════════

export const CATEGORIES: Category[] = [
  { id: "c1", slug: "air-conditioners", name: "Air Conditioners", emoji: "❄️" },
  { id: "c2", slug: "televisions", name: "Televisions", emoji: "📺" },
  { id: "c3", slug: "refrigerators", name: "Refrigerators", emoji: "🧊" },
  { id: "c4", slug: "washing-machines", name: "Washing Machines", emoji: "🌀" },
  { id: "c5", slug: "home-theatre", name: "Home Theatre", emoji: "🔊" },
  { id: "c6", slug: "kitchen", name: "Kitchen & Stove", emoji: "🍳" },
  { id: "c7", slug: "furniture", name: "Furniture", emoji: "🛋️" },
];

export const PRODUCTS: Product[] = [
  {
    id: "p1", slug: "voltas-1-5t-3star-inverter-ac", name: "1.5 Ton 3-Star Inverter Split AC",
    brand: "Voltas", categorySlug: "air-conditioners", emoji: "❄️",
    description: "Energy-efficient inverter AC with copper condenser, turbo cooling and free installation.",
    specs: { Capacity: "1.5 Ton", "Energy Rating": "3 Star Inverter", Warranty: "1 yr + 10 yr compressor", Installation: "Free" },
    price: 34990, mrp: 51500, stock: 2, rating: 4.6, reviewCount: 214, emiPerMonth: 1649, isActive: true, isFeatured: true,
  },
  {
    id: "p2", slug: "sony-55-4k-google-tv", name: '55" 4K Ultra HD Smart Google TV',
    brand: "Sony", categorySlug: "televisions", emoji: "📺",
    description: "Crisp 4K HDR picture with built-in Google TV, voice remote and Dolby Audio.",
    specs: { Display: '55" 4K UHD', "Smart OS": "Google TV", Warranty: "1 year", Connectivity: "Wi-Fi, 3x HDMI" },
    price: 62990, mrp: 87900, stock: 12, rating: 4.4, reviewCount: 176, emiPerMonth: 2950, isActive: true, isFeatured: true,
  },
  {
    id: "p3", slug: "godrej-340l-double-door-fridge", name: "340L Double-Door Frost-Free Fridge",
    brand: "Godrej", categorySlug: "refrigerators", emoji: "🧊",
    description: "Spacious frost-free refrigerator with inverter compressor and toughened glass shelves.",
    specs: { Capacity: "340 L", Type: "Double Door Frost-Free", Warranty: "1 yr + 10 yr compressor", "Energy Rating": "3 Star" },
    price: 41200, mrp: 54000, stock: 8, rating: 4.7, reviewCount: 98, emiPerMonth: 1930, isActive: true, isFeatured: true,
  },
  {
    id: "p4", slug: "lg-7kg-front-load-wm", name: "7Kg Fully Automatic Front-Load Washing Machine",
    brand: "LG", categorySlug: "washing-machines", emoji: "🌀",
    description: "Front-load washer with inverter direct drive, steam wash and 6 motion technology.",
    specs: { Capacity: "7 Kg", Type: "Front Load", Warranty: "2 yr + 10 yr motor", "Wash Programs": "10" },
    price: 28500, mrp: 43900, stock: 6, rating: 4.5, reviewCount: 143, emiPerMonth: 1340, isActive: true, isFeatured: true,
  },
  {
    id: "p5", slug: "jl-home-5-seater-sofa", name: "5-Seater Premium Fabric Sofa Set",
    brand: "JL Home", categorySlug: "furniture", emoji: "🛋️",
    description: "Comfortable 3+2 fabric sofa set with solid wood frame and high-density foam.",
    specs: { Seating: "5-Seater (3+2)", Material: "Premium Fabric", Frame: "Solid Wood", Warranty: "1 year" },
    price: 38000, mrp: 49000, stock: 4, rating: 4.5, reviewCount: 64, emiPerMonth: 1780, isActive: true,
  },
  {
    id: "p6", slug: "jl-home-queen-bed-storage", name: "Queen Size Bed with Storage",
    brand: "JL Home", categorySlug: "furniture", emoji: "🛏️",
    description: "Engineered-wood queen bed with hydraulic storage and a sturdy headboard.",
    specs: { Size: "Queen", Storage: "Hydraulic", Material: "Engineered Wood", Warranty: "1 year" },
    price: 24500, mrp: 31000, stock: 5, rating: 4.3, reviewCount: 41, emiPerMonth: 1150, isActive: true,
  },
  {
    id: "p7", slug: "preethi-mixer-stove-combo", name: "Mixer Grinder + Gas Stove Combo",
    brand: "Preethi", categorySlug: "kitchen", emoji: "🍳",
    description: "750W mixer grinder with 3 jars plus a 2-burner toughened-glass gas stove.",
    specs: { "Mixer Power": "750 W", Jars: "3", Stove: "2 Burner Glass-Top", Warranty: "2 years" },
    price: 9450, mrp: 13200, stock: 15, rating: 4.6, reviewCount: 187, emiPerMonth: 790, isActive: true,
  },
  {
    id: "p8", slug: "sony-5-1-home-theatre", name: "5.1 Channel Home Theatre System",
    brand: "Sony", categorySlug: "home-theatre", emoji: "🔊",
    description: "Immersive 5.1 surround sound with Bluetooth, USB and a powerful subwoofer.",
    specs: { Channels: "5.1", Power: "1000W", Connectivity: "Bluetooth, USB, HDMI", Warranty: "1 year" },
    price: 18990, mrp: 26500, stock: 3, rating: 4.4, reviewCount: 72, emiPerMonth: 890, isActive: true,
  },
];

// ── Getters (sync, seed-backed). Replace with Supabase in Phase 2. ──
export function getCategories(): Category[] {
  return CATEGORIES;
}

export function getProducts(categorySlug?: string): Product[] {
  const active = PRODUCTS.filter((p) => p.isActive);
  return categorySlug ? active.filter((p) => p.categorySlug === categorySlug) : active;
}

export function getFeatured(): Product[] {
  return PRODUCTS.filter((p) => p.isActive && p.isFeatured);
}

export function getProductBySlug(slug: string): Product | undefined {
  return PRODUCTS.find((p) => p.slug === slug && p.isActive);
}

export function getCategoryBySlug(slug: string): Category | undefined {
  return CATEGORIES.find((c) => c.slug === slug);
}
