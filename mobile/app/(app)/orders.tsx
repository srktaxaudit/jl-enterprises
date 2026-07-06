import { FlatList, Pressable, RefreshControl, View } from "react-native";
import { useRouter } from "expo-router";
import {
  AppText,
  Card,
  EmptyState,
  ErrorView,
  LoadingView,
  Screen,
  StatusBadge,
} from "@/shared/components/ui";
import { orderTone, useOrders, type OrderSummary } from "@/features/orders/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateTime, inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

function OrderRow({ o, onPress }: { o: OrderSummary; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} style={({ pressed }) => ({ opacity: pressed ? 0.7 : 1 })}>
      <Card style={{ padding: 14 }}>
        <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
          <AppText weight="800" size={15}>#{o.orderNumber}</AppText>
          <AppText weight="800" size={15}>{inr(o.grandTotal)}</AppText>
        </View>
        <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginTop: 8 }}>
          <StatusBadge label={o.status} tone={orderTone(o.status)} />
          <AppText muted size={12}>{o.itemCount} item(s) · {dateTime(o.placedAt ?? o.createdAt)}</AppText>
        </View>
      </Card>
    </Pressable>
  );
}

export default function Orders() {
  const t = useTheme();
  const router = useRouter();
  const {
    data,
    isLoading,
    isError,
    error,
    refetch,
    isRefetching,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useOrders();

  if (isLoading) return <Screen><LoadingView label="Loading orders…" /></Screen>;
  if (isError) {
    return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;
  }

  const rows = data?.pages.flatMap((p) => p.content) ?? [];

  return (
    <Screen padded={false}>
      <FlatList
        data={rows}
        keyExtractor={(o) => o.id}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        onEndReachedThreshold={0.4}
        onEndReached={() => {
          if (hasNextPage && !isFetchingNextPage) void fetchNextPage();
        }}
        ListEmptyComponent={<EmptyState title="No orders yet" hint="Orders placed in the store will appear here." />}
        ListFooterComponent={isFetchingNextPage ? <View style={{ paddingVertical: 16 }}><LoadingView label="Loading more…" /></View> : null}
        renderItem={({ item }) => <OrderRow o={item} onPress={() => router.push(`/order/${item.id}`)} />}
      />
    </Screen>
  );
}
