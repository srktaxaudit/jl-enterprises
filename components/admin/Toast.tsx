"use client";

import { createContext, useContext, useState, useCallback, ReactNode } from "react";

const Ctx = createContext<(msg: string) => void>(() => {});
export const useToast = () => useContext(Ctx);

export function ToastHost({ children }: { children: ReactNode }) {
  const [msg, setMsg] = useState("");
  const [show, setShow] = useState(false);
  const toast = useCallback((m: string) => {
    setMsg(m); setShow(true);
    setTimeout(() => setShow(false), 2200);
  }, []);
  return (
    <Ctx.Provider value={toast}>
      {children}
      <div className={`fixed bottom-6 left-1/2 -translate-x-1/2 bg-navy text-white px-5 py-3 rounded-full text-sm shadow-xl transition-opacity z-50 ${show ? "opacity-100" : "opacity-0 pointer-events-none"}`}>
        {msg}
      </div>
    </Ctx.Provider>
  );
}
