import { FlatList, RefreshControl, View } from "react-native";
import { AppText, Card, EmptyState, ErrorView, LoadingView, Screen, StatusBadge } from "@/shared/components/ui";
import { payTone, useBillingSummary, useInvoices } from "@/features/billing/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateOnly, inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

function SummaryCard() {
  const { data } = useBillingSummary();
  if (!data) return null;
  const stats: Array<[string, string]> = [
    ["Gross revenue", inr(data.grossRevenue)],
    ["Net collected", inr(data.netCollected)],
    ["Paid", `${inr(data.paidTotal)} (${data.paidCount})`],
    ["Pending", `${inr(data.pendingTotal)} (${data.pendingCount})`],
    ["Tax collected", inr(data.taxCollected)],
    ["Refunded", `${inr(data.refundedTotal)} (${data.refundedCount})`],
  ];
  return (
    <Card style={{ padding: 14, marginBottom: 12 }}>
      <AppText weight="800" size={15} style={{ marginBottom: 8 }}>Summary</AppText>
      <View style={{ flexDirection: "row", flexWrap: "wrap" }}>
        {stats.map(([k, v]) => (
          <View key={k} style={{ width: "50%", paddingVertical: 6 }}>
            <AppText muted size={11}>{k.toUpperCase()}</AppText>
            <AppText weight="700" size={14} style={{ marginTop: 2 }}>{v}</AppText>
          </View>
        ))}
      </View>
    </Card>
  );
}

export default function Billing() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useInvoices();

  if (isLoading) return <Screen><LoadingView label="Loading billing…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  const rows = data?.pages.flatMap((p) => p.content) ?? [];

  return (
    <Screen padded={false}>
      <FlatList
        data={rows}
        keyExtractor={(b) => b.orderId}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        onEndReachedThreshold={0.4}
        onEndReached={() => { if (hasNextPage && !isFetchingNextPage) void fetchNextPage(); }}
        ListHeaderComponent={<SummaryCard />}
        ListEmptyComponent={<EmptyState emoji="🧾" title="No invoices" />}
        ListFooterComponent={isFetchingNextPage ? <View style={{ paddingVertical: 16 }}><LoadingView label="Loading more…" /></View> : null}
        renderItem={({ item }) => (
          <Card style={{ padding: 14 }}>
            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
              <AppText weight="800" size={14}>#{item.orderNumber}</AppText>
              <AppText weight="700" size={14}>{inr(item.grandTotal)}</AppText>
            </View>
            <AppText muted size={12} style={{ marginTop: 4 }} numberOfLines={1}>
              {item.customerName ?? item.customerEmail ?? "—"} · {dateOnly(item.placedAt)}
            </AppText>
            <View style={{ flexDirection: "row", gap: 8, marginTop: 8, alignItems: "center" }}>
              {item.paymentStatus ? <StatusBadge label={item.paymentStatus} tone={payTone(item.paymentStatus)} /> : null}
              {item.paymentMethod ? <AppText muted size={12}>{item.paymentMethod}</AppText> : null}
            </View>
          </Card>
        )}
      />
    </Screen>
  );
}
