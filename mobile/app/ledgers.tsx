import { useState } from "react";
import { FlatList, Modal, Pressable, RefreshControl, ScrollView, View } from "react-native";
import { AppText, Button, Card, EmptyState, ErrorView, LoadingView, Screen } from "@/shared/components/ui";
import { useAccounts, useLedger } from "@/features/accounting/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateOnly, inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

export default function Ledgers() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching } = useAccounts();
  const [accountId, setAccountId] = useState<string | undefined>(undefined);
  const ledger = useLedger(accountId);

  if (isLoading) return <Screen><LoadingView label="Loading accounts…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  return (
    <Screen padded={false}>
      <FlatList
        data={data ?? []}
        keyExtractor={(a) => a.id}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        ListHeaderComponent={<AppText muted size={13} style={{ marginBottom: 6 }}>Tap an account to view its ledger (last 12 months).</AppText>}
        ListEmptyComponent={<EmptyState emoji="📚" title="No accounts" />}
        renderItem={({ item }) => (
          <Pressable onPress={() => setAccountId(item.id)} style={({ pressed }) => ({ opacity: pressed ? 0.7 : 1 })}>
            <Card style={{ padding: 14, flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
              <AppText weight="700" size={14} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>{item.code} · {item.name}</AppText>
              <AppText muted size={16}>›</AppText>
            </Card>
          </Pressable>
        )}
      />

      <Modal visible={!!accountId} transparent animationType="slide" onRequestClose={() => setAccountId(undefined)}>
        <View style={{ flex: 1, backgroundColor: "#0006", justifyContent: "flex-end" }}>
          <View style={{ backgroundColor: t.bg, borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, maxHeight: "85%" }}>
            {ledger.isLoading ? (
              <LoadingView label="Loading ledger…" />
            ) : ledger.isError || !ledger.data ? (
              <ErrorView message={ledger.error instanceof ApiError ? ledger.error.message : "Network error"} onRetry={ledger.refetch} />
            ) : (
              <>
                <AppText weight="800" size={16}>{ledger.data.accountName}</AppText>
                <AppText muted size={12} style={{ marginBottom: 10 }}>
                  {dateOnly(ledger.data.from)} → {dateOnly(ledger.data.to)} · Opening {inr(ledger.data.openingBalance)}
                </AppText>
                <ScrollView style={{ maxHeight: 380 }}>
                  {ledger.data.lines.map((ln: any, i: number) => (
                    <View key={i} style={{ paddingVertical: 6, borderBottomWidth: 1, borderBottomColor: t.border }}>
                      <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
                        <AppText size={12} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>{ln.voucherNumber ?? "—"} · {dateOnly(ln.date)}</AppText>
                        <AppText size={12} weight="600">{ln.debit ? `Dr ${inr(ln.debit)}` : `Cr ${inr(ln.credit)}`}</AppText>
                      </View>
                      {ln.narration ? <AppText muted size={11} numberOfLines={1}>{ln.narration}</AppText> : null}
                      <AppText muted size={11}>Balance {inr(ln.runningBalance)}</AppText>
                    </View>
                  ))}
                  {!ledger.data.lines.length ? <AppText muted size={13} style={{ textAlign: "center", paddingVertical: 20 }}>No entries in this period.</AppText> : null}
                </ScrollView>
                <View style={{ flexDirection: "row", justifyContent: "space-between", marginTop: 10 }}>
                  <AppText weight="800" size={14}>Closing</AppText>
                  <AppText weight="800" size={14}>{inr(ledger.data.closingBalance)}</AppText>
                </View>
              </>
            )}
            <View style={{ height: 12 }} />
            <Button title="Close" variant="outline" onPress={() => setAccountId(undefined)} />
          </View>
        </View>
      </Modal>
    </Screen>
  );
}
