import { useQuery } from "@tanstack/react-query";
import { apiGet } from "@/core/api/client";

// ── GSTR-3B summary (Gstr3bDto) ──
export interface Gstr3b {
  from?: string;
  to?: string;
  outwardTaxable: number;
  outwardCgst: number;
  outwardSgst: number;
  outwardIgst: number;
  inwardTaxable: number;
  itcCgst: number;
  itcSgst: number;
  itcIgst: number;
  netCgst: number;
  netSgst: number;
  netIgst: number;
  netPayable: number;
}

export function useGstr3b() {
  return useQuery({ queryKey: ["compliance", "gstr3b"], queryFn: () => apiGet<Gstr3b>("/api/v1/admin/compliance/gstr3b") });
}

// ── Aging (AgingReportDto) ──
export interface AgingParty {
  partyName: string;
  current: number;
  days31to60: number;
  days61to90: number;
  days90plus: number;
  total: number;
  oldestDays: number;
}
export interface AgingReport {
  kind: string; // RECEIVABLE | PAYABLE
  asOf?: string;
  parties: AgingParty[];
  current: number;
  days31to60: number;
  days61to90: number;
  days90plus: number;
  total: number;
}

export function useAging(kind: "RECEIVABLE" | "PAYABLE") {
  return useQuery({
    queryKey: ["compliance", "aging", kind],
    queryFn: () => apiGet<AgingReport>("/api/v1/admin/compliance/aging", { kind }),
  });
}

// ── Cash flow (CashFlowDto) ──
export interface CashFlowRow { date: string; voucherNumber?: string; particulars: string; amount: number }
export interface CashFlow {
  from?: string;
  to?: string;
  openingBalance: number;
  inflows: CashFlowRow[];
  outflows: CashFlowRow[];
  totalInflows: number;
  totalOutflows: number;
  closingBalance: number;
}

export function useCashFlow() {
  return useQuery({ queryKey: ["compliance", "cashflow"], queryFn: () => apiGet<CashFlow>("/api/v1/admin/compliance/cash-flow") });
}
