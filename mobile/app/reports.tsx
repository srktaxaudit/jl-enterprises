import { useState } from "react";
import { Pressable, RefreshControl, ScrollView, View } from "react-native";
import { AppText, Card, ErrorView, LoadingView, Screen } from "@/shared/components/ui";
import {
  useBalanceSheet,
  usePnl,
  useTrialBalance,
  type FinancialStatement,
  type TrialBalance,
} from "@/features/accounting/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateOnly, inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

const TABS = ["P&L", "Balance Sheet", "Trial Balance"] as const;

function Statement({ s }: { s: FinancialStatement }) {
  return (
    <View style={{ gap: 12 }}>
      <AppText muted size={12}>{dateOnly(s.from)} → {dateOnly(s.to)}</AppText>
      {s.sections.map((sec) => (
        <Card key={sec.name} style={{ padding: 14 }}>
          <AppText weight="800" size={14} style={{ marginBottom: 6 }}>{sec.name}</AppText>
          {sec.lines.map((ln, i) => (
            <View key={ln.code ?? i} style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 3 }}>
              <AppText size={13} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>{ln.name}</AppText>
              <AppText size={13}>{inr(ln.amount)}</AppText>
            </View>
          ))}
          <View style={{ flexDirection: "row", justifyContent: "space-between", marginTop: 8, borderTopWidth: 1, borderTopColor: "#8883", paddingTop: 6 }}>
            <AppText weight="700" size={13}>Total</AppText>
            <AppText weight="700" size={13}>{inr(sec.total)}</AppText>
          </View>
        </Card>
      ))}
      <Card style={{ padding: 14, flexDirection: "row", justifyContent: "space-between" }}>
        <AppText weight="800" size={15}>{s.resultLabel}</AppText>
        <AppText weight="800" size={15}>{inr(s.result)}</AppText>
      </Card>
    </View>
  );
}

function TrialBalanceView({ tb }: { tb: TrialBalance }) {
  return (
    <Card style={{ padding: 14 }}>
      <AppText muted size={12} style={{ marginBottom: 8 }}>As of {dateOnly(tb.asOf)}</AppText>
      <View style={{ flexDirection: "row", justifyContent: "space-between", marginBottom: 6 }}>
        <AppText muted size={11} weight="700" style={{ flex: 1 }}>ACCOUNT</AppText>
        <AppText muted size={11} weight="700">DR</AppText>
        <AppText muted size={11} weight="700" style={{ width: 80, textAlign: "right" }}>CR</AppText>
      </View>
      {tb.rows.map((r) => (
        <View key={r.code} style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 3 }}>
          <AppText size={12} style={{ flex: 1, paddingRight: 6 }} numberOfLines={1}>{r.name}</AppText>
          <AppText size={12}>{r.debit ? inr(r.debit) : "—"}</AppText>
          <AppText size={12} style={{ width: 80, textAlign: "right" }}>{r.credit ? inr(r.credit) : "—"}</AppText>
        </View>
      ))}
      <View style={{ flexDirection: "row", justifyContent: "space-between", marginTop: 8, borderTopWidth: 1, borderTopColor: "#8883", paddingTop: 6 }}>
        <AppText weight="700" size={13} style={{ flex: 1 }}>Total</AppText>
        <AppText weight="700" size={13}>{inr(tb.totalDebit)}</AppText>
        <AppText weight="700" size={13} style={{ width: 80, textAlign: "right" }}>{inr(tb.totalCredit)}</AppText>
      </View>
    </Card>
  );
}

export default function Reports() {
  const t = useTheme();
  const [tab, setTab] = useState(0);
  const pnl = usePnl();
  const bs = useBalanceSheet();
  const tb = useTrialBalance();
  const active = tab === 0 ? pnl : tab === 1 ? bs : tb;

  return (
    <Screen padded={false}>
      <View style={{ flexDirection: "row", gap: 8, padding: 16, paddingBottom: 8 }}>
        {TABS.map((label, i) => {
          const on = i === tab;
          return (
            <Pressable key={label} onPress={() => setTab(i)} style={{ flex: 1, paddingVertical: 9, borderRadius: 10, alignItems: "center", backgroundColor: on ? t.primary : t.surface, borderWidth: 1, borderColor: on ? t.primary : t.border }}>
              <AppText size={12} weight="700" color={on ? t.onPrimary : t.textMuted}>{label}</AppText>
            </Pressable>
          );
        })}
      </View>
      {active.isLoading ? (
        <LoadingView label="Loading report…" />
      ) : active.isError ? (
        <ErrorView message={active.error instanceof ApiError ? active.error.message : "Network error"} onRetry={active.refetch} />
      ) : (
        <ScrollView
          contentContainerStyle={{ padding: 16, paddingTop: 4 }}
          refreshControl={<RefreshControl refreshing={active.isRefetching} onRefresh={active.refetch} tintColor={t.accent} />}
        >
          {tab === 2
            ? tb.data ? <TrialBalanceView tb={tb.data} /> : null
            : (tab === 0 ? pnl.data : bs.data) ? <Statement s={(tab === 0 ? pnl.data : bs.data) as FinancialStatement} /> : null}
        </ScrollView>
      )}
    </Screen>
  );
}
