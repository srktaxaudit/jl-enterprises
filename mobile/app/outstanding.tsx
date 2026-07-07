import { useState } from "react";
import { Pressable, RefreshControl, ScrollView, View } from "react-native";
import { AppText, Card, ErrorView, LoadingView, Screen } from "@/shared/components/ui";
import { useAging, useCashFlow } from "@/features/compliance/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

const KINDS = ["RECEIVABLE", "PAYABLE"] as const;

export default function Outstanding() {
  const t = useTheme();
  const [kind, setKind] = useState<(typeof KINDS)[number]>("RECEIVABLE");
  const aging = useAging(kind);
  const cash = useCashFlow();

  return (
    <Screen padded={false}>
      <View style={{ flexDirection: "row", gap: 8, padding: 16, paddingBottom: 8 }}>
        {KINDS.map((k) => {
          const on = k === kind;
          return (
            <Pressable key={k} onPress={() => setKind(k)} style={{ flex: 1, paddingVertical: 9, borderRadius: 10, alignItems: "center", backgroundColor: on ? t.primary : t.surface, borderWidth: 1, borderColor: on ? t.primary : t.border }}>
              <AppText size={12} weight="700" color={on ? t.onPrimary : t.textMuted}>{k}</AppText>
            </Pressable>
          );
        })}
      </View>

      {aging.isLoading ? (
        <LoadingView label="Loading…" />
      ) : aging.isError || !aging.data ? (
        <ErrorView message={aging.error instanceof ApiError ? aging.error.message : "Network error"} onRetry={aging.refetch} />
      ) : (
        <ScrollView
          contentContainerStyle={{ padding: 16, paddingTop: 4, gap: 12 }}
          refreshControl={<RefreshControl refreshing={aging.isRefetching} onRefresh={() => { void aging.refetch(); void cash.refetch(); }} tintColor={t.accent} />}
        >
          {cash.data ? (
            <Card style={{ padding: 14 }}>
              <AppText weight="800" size={14} style={{ marginBottom: 6 }}>Cash &amp; bank</AppText>
              <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 3 }}><AppText size={13} muted>Opening</AppText><AppText size={13}>{inr(cash.data.openingBalance)}</AppText></View>
              <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 3 }}><AppText size={13} muted>Inflows</AppText><AppText size={13} color={t.success}>{inr(cash.data.totalInflows)}</AppText></View>
              <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 3 }}><AppText size={13} muted>Outflows</AppText><AppText size={13} color={t.danger}>{inr(cash.data.totalOutflows)}</AppText></View>
              <View style={{ flexDirection: "row", justifyContent: "space-between", marginTop: 6, borderTopWidth: 1, borderTopColor: t.border, paddingTop: 6 }}><AppText weight="700" size={13}>Closing</AppText><AppText weight="700" size={13}>{inr(cash.data.closingBalance)}</AppText></View>
            </Card>
          ) : null}

          <Card style={{ padding: 14 }}>
            <AppText weight="800" size={14} style={{ marginBottom: 6 }}>Aging buckets</AppText>
            <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 3 }}><AppText size={13} muted>Current</AppText><AppText size={13}>{inr(aging.data.current)}</AppText></View>
            <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 3 }}><AppText size={13} muted>31–60 days</AppText><AppText size={13}>{inr(aging.data.days31to60)}</AppText></View>
            <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 3 }}><AppText size={13} muted>61–90 days</AppText><AppText size={13}>{inr(aging.data.days61to90)}</AppText></View>
            <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 3 }}><AppText size={13} muted>90+ days</AppText><AppText size={13} color={t.danger}>{inr(aging.data.days90plus)}</AppText></View>
            <View style={{ flexDirection: "row", justifyContent: "space-between", marginTop: 6, borderTopWidth: 1, borderTopColor: t.border, paddingTop: 6 }}><AppText weight="700" size={13}>Total</AppText><AppText weight="700" size={13}>{inr(aging.data.total)}</AppText></View>
          </Card>

          {aging.data.parties.map((p) => (
            <Card key={p.partyName} style={{ padding: 14 }}>
              <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                <AppText weight="700" size={13} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>{p.partyName}</AppText>
                <AppText weight="700" size={13}>{inr(p.total)}</AppText>
              </View>
              <AppText muted size={12} style={{ marginTop: 4 }}>Oldest {p.oldestDays} days · 90+: {inr(p.days90plus)}</AppText>
            </Card>
          ))}
          {!aging.data.parties.length ? <AppText muted size={13} style={{ textAlign: "center" }}>Nothing outstanding.</AppText> : null}
        </ScrollView>
      )}
    </Screen>
  );
}
