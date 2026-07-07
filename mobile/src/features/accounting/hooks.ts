import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { apiGet } from "@/core/api/client";
import type { PageResponse } from "@/core/types";

// ── Chart of accounts (LedgerAccountDto) ──
export interface LedgerAccount {
  id: string;
  code: string;
  name: string;
  accountGroup: string;
  openingBalance: number;
  gstRate?: number;
  gstin?: string;
  hsnCode?: string;
  creditLimit?: number;
  creditDays?: number;
  blocked: boolean;
  active: boolean;
  systemAccount: boolean;
}

export function useAccounts() {
  return useQuery({
    queryKey: ["accounting", "accounts"],
    queryFn: () => apiGet<LedgerAccount[]>("/api/v1/admin/accounting/accounts", { includeInactive: true }),
  });
}

// ── Journal entries (JournalEntryDto) ──
export interface JournalLine {
  id?: string;
  accountId: string;
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
  lineNarration?: string;
}
export interface JournalEntry {
  id: string;
  voucherNumber: string;
  entryDate: string;
  narration?: string;
  reference?: string;
  referenceId?: string;
  lines: JournalLine[];
  totalDebit: number;
  totalCredit: number;
}

export function useJournals() {
  return useInfiniteQuery({
    queryKey: ["accounting", "journals"],
    queryFn: ({ pageParam }) =>
      apiGet<PageResponse<JournalEntry>>("/api/v1/admin/accounting/journals", { page: pageParam, size: 20 }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  });
}

// ── Ledger statement (LedgerStatementDto) ──
export interface LedgerStatementLine {
  date: string;
  voucherNumber?: string;
  narration?: string;
  debit: number;
  credit: number;
  runningBalance: number;
}
export interface LedgerStatement {
  accountId: string;
  accountCode: string;
  accountName: string;
  from?: string;
  to?: string;
  openingBalance: number;
  lines: LedgerStatementLine[];
  closingBalance: number;
}

export function useLedger(accountId?: string) {
  return useQuery({
    queryKey: ["accounting", "ledger", accountId],
    queryFn: () => apiGet<LedgerStatement>(`/api/v1/admin/accounting/ledger/${accountId}`),
    enabled: !!accountId,
  });
}

// ── Reports (FinancialStatementDto / TrialBalanceDto) ──
export interface FinLine { code?: string; name: string; amount: number }
export interface FinSection { name: string; lines: FinLine[]; total: number }
export interface FinancialStatement {
  title: string;
  from?: string;
  to?: string;
  sections: FinSection[];
  resultLabel: string;
  result: number;
}
export interface TbRow { code: string; name: string; debit: number; credit: number }
export interface TrialBalance {
  asOf: string;
  rows: TbRow[];
  totalDebit: number;
  totalCredit: number;
}

export function usePnl() {
  return useQuery({ queryKey: ["accounting", "pnl"], queryFn: () => apiGet<FinancialStatement>("/api/v1/admin/accounting/reports/pnl") });
}
export function useBalanceSheet() {
  return useQuery({ queryKey: ["accounting", "bs"], queryFn: () => apiGet<FinancialStatement>("/api/v1/admin/accounting/reports/balance-sheet") });
}
export function useTrialBalance() {
  return useQuery({ queryKey: ["accounting", "tb"], queryFn: () => apiGet<TrialBalance>("/api/v1/admin/accounting/reports/trial-balance") });
}
