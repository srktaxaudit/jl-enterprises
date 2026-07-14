import React from "react";
import { FlatList, Pressable, View } from "react-native";
import { useRouter } from "expo-router";
import { AppText, Card, EmptyState, ErrorView, LoadingView, Screen, StatusBadge } from "@/shared/components/ui";
import { SignInPrompt } from "@/shared/components/SignInPrompt";
import { useAuth } from "@/core/auth/authStore";
import { useMyOrders } from "@/features/orders/hooks";
import { statusTone } from "@/shared/orderStatus";
import { dateTime, inr, statusLabel } from "@/shared/format";

export default function OrdersTab() {
  const router = useRouter();
  const status = useAuth((s) => s.status);
  const orders = useMyOrders();

  if (status === "loading") return <Screen><LoadingView /></Screen>;
  if (status === "guest") {
    return (
      <Screen>
        <SignInPrompt message="Sign in to see your orders and track deliveries." />
      </Screen>
    );
  }
  if (orders.isLoading) return <Screen><LoadingView label="Loading orders…" /></Screen>;
  if (orders.isError) {
    return <Screen><ErrorView message={(orders.error as Error).message} onRetry={() => orders.refetch()} /></Screen>;
  }

  const items = (orders.data?.pages ?? []).flatMap((p) => p.content);
  if (items.length === 0) {
    return <Screen><EmptyState emoji="📦" title="No orders yet" hint="Your orders will show up here after checkout." /></Screen>;
  }

  return (
    <Screen>
      <FlatList
        data={items}
        keyExtractor={(o) => o.id}
        refreshing={orders.isRefetching}
        onRefresh={() => void orders.refetch()}
        onEndReachedThreshold={0.4}
        onEndReached={() => {
          if (orders.hasNextPage && !orders.isFetchingNextPage) void orders.fetchNextPage();
        }}
        ListFooterComponent={orders.isFetchingNextPage ? <LoadingView label="Loading more…" /> : null}
        renderItem={({ item }) => (
          <Pressable onPress={() => router.push(`/order/${item.id}`)}>
            <Card style={{ marginBottom: 10, gap: 8 }}>
              <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                <AppText weight="700" size={14}>#{item.orderNumber}</AppText>
                <StatusBadge label={statusLabel(item.status)} tone={statusTone(item.status)} />
              </View>
              <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                <AppText muted size={12}>{item.itemCount} item{item.itemCount === 1 ? "" : "s"} · {dateTime(item.placedAt)}</AppText>
                <AppText weight="800" size={15}>{inr(item.grandTotal)}</AppText>
              </View>
            </Card>
          </Pressable>
        )}
      />
    </Screen>
  );
}
