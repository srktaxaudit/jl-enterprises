"use client";

import { useState } from "react";
import AdminSidebar from "@/components/admin/AdminSidebar";
import { LogoMark } from "@/components/Logo";

export default function AdminShell({ children }: { children: React.ReactNode }) {
  const [open, setOpen] = useState(false);

  return (
    <div className="md:flex bg-[#eef1f6] min-h-screen">
      {/* Desktop sidebar */}
      <aside className="hidden md:block w-[248px] shrink-0 bg-gradient-to-b from-[#0b2447] to-[#19376d] sticky top-0 h-screen py-5">
        <AdminSidebar />
      </aside>

      {/* Mobile drawer */}
      {open && (
        <div className="md:hidden fixed inset-0 z-40 bg-black/40" onClick={() => setOpen(false)} />
      )}
      <aside
        className={`md:hidden fixed z-50 top-0 left-0 h-full w-[260px] bg-gradient-to-b from-[#0b2447] to-[#19376d] py-5 transition-transform duration-200 ${
          open ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <AdminSidebar onNavigate={() => setOpen(false)} />
      </aside>

      <div className="flex-1 min-w-0">
        {/* Top bar */}
        <div className="bg-white border-b border-slate-200 px-4 md:px-7 py-3 flex items-center gap-3 sticky top-0 z-10">
          <button
            onClick={() => setOpen(true)}
            className="md:hidden w-9 h-9 rounded-lg border border-slate-200 flex items-center justify-center text-navy text-lg"
            aria-label="Open menu"
          >
            ☰
          </button>
          <div className="md:hidden flex items-center gap-2">
            <LogoMark className="w-9 h-7" />
            <span className="font-bold text-navy text-sm">Admin</span>
          </div>
          <div className="ml-auto flex items-center gap-3">
            <span className="hidden sm:flex items-center gap-2 bg-green-50 text-green-700 px-3 py-1.5 rounded-full text-[12px] font-semibold">
              <span className="w-2 h-2 rounded-full bg-green-500" /> 27 online
            </span>
            <span className="w-9 h-9 rounded-full bg-gradient-to-br from-navy-600 to-brand text-white flex items-center justify-center font-bold text-[13px]">CEO</span>
          </div>
        </div>

        <div className="p-4 md:p-7">{children}</div>
      </div>
    </div>
  );
}
