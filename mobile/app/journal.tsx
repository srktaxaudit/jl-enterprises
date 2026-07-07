import { FlatList, RefreshControl, View } from "react-native";
import { AppText, Card, EmptyState, ErrorView, LoadingView, Screen } from "@/shared/components/ui";
import { useJournals } from "@/features/accounting/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateOnly, inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

export default function Journal() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useJournals();

  if (isLoading) return <Screen><LoadingView label="Loading journal…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  const rows = data?.pages.flatMap((p) => p.content) ?? [];

  return (
    <Screen padded={false}>
      <FlatList
        data={rows}
        keyExtractor={(j) => j.id}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        onEndReachedThreshold={0.4}
        onEndReached={() => { if (hasNextPage && !isFetchingNextPage) void fetchNextPage(); }}
        ListEmptyComponent={<EmptyState emoji="✍️" title="No journal entries" />}
        ListFooterComponent={isFetchingNextPage ? <View style={{ paddingVertical: 16 }}><LoadingView label="Loading more…" /></View> : null}
        renderItem={({ item }) => (
          <Card style={{ padding: 14 }}>
            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
              <AppText weight="800" size={14}>{item.voucherNumber}</AppText>
              <AppText weight="700" size={13}>{inr(item.totalDebit)}</AppText>
            </View>
            <AppText muted size={12} style={{ marginTop: 4 }}>{dateOnly(item.entryDate)}{item.reference ? ` · ${item.reference}` : ""}</AppText>
            {item.narration ? <AppText size={13} style={{ marginTop: 4 }} numberOfLines={2}>{item.narration}</AppText> : null}
            <View style={{ marginTop: 8, gap: 2 }}>
              {item.lines?.slice(0, 4).map((ln, i) => (
                <View key={ln.id ?? i} style={{ flexDirection: "row", justifyContent: "space-between" }}>
                  <AppText muted size={12} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>{ln.accountName}</AppText>
                  <AppText muted size={12}>{ln.debit ? `Dr ${inr(ln.debit)}` : `Cr ${inr(ln.credit)}`}</AppText>
                </View>
              ))}
            </View>
          </Card>
        )}
      />
    </Screen>
  );
}
