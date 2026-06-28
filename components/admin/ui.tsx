import { STATUS_LABEL } from "@/lib/admin-demo";

export function StatCard({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4">
      <div className="text-[12px] text-slate-400 uppercase tracking-wide">{label}</div>
      <div className="text-2xl font-extrabold text-navy mt-1">{value}</div>
      {sub && <div className="text-[12px] text-slate-400 mt-0.5">{sub}</div>}
    </div>
  );
}

const STATUS_CLASS: Record<string, string> = {
  NEW: "bg-pink-100 text-pink-700",
  PACKED: "bg-amber-100 text-amber-700",
  OUT_FOR_DELIVERY: "bg-blue-100 text-blue-700",
  DELIVERED: "bg-green-100 text-green-700",
  CANCELLED: "bg-slate-200 text-slate-600",
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <span className={`text-[11px] font-bold px-2.5 py-1 rounded-full whitespace-nowrap ${STATUS_CLASS[status] ?? "bg-slate-100 text-slate-600"}`}>
      {STATUS_LABEL[status] ?? status}
    </span>
  );
}

export function PageHead({ title, sub }: { title: string; sub?: string }) {
  return (
    <div className="mb-5">
      <h2 className="text-2xl text-navy font-bold">{title}</h2>
      {sub && <p className="text-slate-400 text-sm">{sub}</p>}
    </div>
  );
}
