import { FlatList, RefreshControl, View } from "react-native";
import { AppText, Card, EmptyState, ErrorView, LoadingView, Screen, StatusBadge } from "@/shared/components/ui";
import { useDocuments } from "@/features/documents/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateOnly, inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

export default function Vouchers() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useDocuments();

  if (isLoading) return <Screen><LoadingView label="Loading documents…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  const rows = data?.pages.flatMap((p) => p.content) ?? [];

  return (
    <Screen padded={false}>
      <FlatList
        data={rows}
        keyExtractor={(d) => d.id}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        onEndReachedThreshold={0.4}
        onEndReached={() => { if (hasNextPage && !isFetchingNextPage) void fetchNextPage(); }}
        ListHeaderComponent={<AppText muted size={13} style={{ marginBottom: 6 }}>Invoices &amp; bills. Create new documents on the web admin.</AppText>}
        ListEmptyComponent={<EmptyState emoji="🧾" title="No documents" />}
        ListFooterComponent={isFetchingNextPage ? <View style={{ paddingVertical: 16 }}><LoadingView label="Loading more…" /></View> : null}
        renderItem={({ item }) => (
          <Card style={{ padding: 14 }}>
            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
              <AppText weight="800" size={14}>{item.documentNumber}</AppText>
              <AppText weight="700" size={14}>{inr(item.grandTotal)}</AppText>
            </View>
            <AppText muted size={12} style={{ marginTop: 4 }} numberOfLines={1}>
              {item.partyName ?? "—"} · {dateOnly(item.documentDate)}
            </AppText>
            <View style={{ flexDirection: "row", gap: 8, marginTop: 8, alignItems: "center" }}>
              {item.documentType ? <StatusBadge label={item.documentType} tone="info" /> : null}
              {item.status ? <StatusBadge label={item.status} tone="muted" /> : null}
              <AppText muted size={12}>GST {inr(item.gstTotal)}</AppText>
            </View>
          </Card>
        )}
      />
    </Screen>
  );
}
