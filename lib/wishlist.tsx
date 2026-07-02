"use client";

import { createContext, useContext, useEffect, useState, ReactNode } from "react";
import type { Product } from "./types";

// localStorage-backed wishlist, mirroring the cart provider so it works
// for guests and needs no database.

type WishlistCtx = {
  items: Product[];
  count: number;
  has: (id: string) => boolean;
  toggle: (p: Product) => void;
  remove: (id: string) => void;
};

const Ctx = createContext<WishlistCtx | null>(null);
const KEY = "jl_wishlist_v1";

export function WishlistProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<Product[]>([]);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(KEY);
      if (raw) setItems(JSON.parse(raw));
    } catch {}
    setReady(true);
  }, []);

  useEffect(() => {
    if (ready) localStorage.setItem(KEY, JSON.stringify(items));
  }, [items, ready]);

  const has = (id: string) => items.some((p) => p.id === id);

  const toggle = (p: Product) =>
    setItems((prev) =>
      prev.some((x) => x.id === p.id) ? prev.filter((x) => x.id !== p.id) : [...prev, p]
    );

  const remove = (id: string) => setItems((prev) => prev.filter((x) => x.id !== id));

  return (
    <Ctx.Provider value={{ items, count: items.length, has, toggle, remove }}>
      {children}
    </Ctx.Provider>
  );
}

export function useWishlist() {
  const c = useContext(Ctx);
  if (!c) throw new Error("useWishlist must be used inside <WishlistProvider>");
  return c;
}
