import { FlatList, RefreshControl, View } from "react-native";
import { AppText, Card, EmptyState, ErrorView, LoadingView, Screen } from "@/shared/components/ui";
import { useAuditLogs } from "@/features/logs/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateTime } from "@/shared/format";
import { ApiError } from "@/core/api/client";

export default function Logs() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useAuditLogs();

  if (isLoading) return <Screen><LoadingView label="Loading activity…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  const rows = data?.pages.flatMap((p) => p.content) ?? [];

  return (
    <Screen padded={false}>
      <FlatList
        data={rows}
        keyExtractor={(l) => l.id}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        onEndReachedThreshold={0.4}
        onEndReached={() => { if (hasNextPage && !isFetchingNextPage) void fetchNextPage(); }}
        ListEmptyComponent={<EmptyState emoji="📜" title="No activity yet" />}
        ListFooterComponent={isFetchingNextPage ? <View style={{ paddingVertical: 16 }}><LoadingView label="Loading more…" /></View> : null}
        renderItem={({ item }) => (
          <Card style={{ padding: 14 }}>
            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
              <AppText weight="700" size={13} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>{item.action}</AppText>
              <AppText muted size={11}>{dateTime(item.createdAt)}</AppText>
            </View>
            <AppText muted size={12} style={{ marginTop: 4 }}>
              {item.actor ?? "system"}{item.entity ? ` · ${item.entity}` : ""}
            </AppText>
            {item.detail ? <AppText size={12} style={{ marginTop: 4 }} numberOfLines={3}>{item.detail}</AppText> : null}
          </Card>
        )}
      />
    </Screen>
  );
}
