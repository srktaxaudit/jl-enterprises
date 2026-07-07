import { useState } from "react";
import { FlatList, Modal, Pressable, RefreshControl, View } from "react-native";
import { AppText, Button, Card, EmptyState, ErrorView, LoadingView, Screen, StatusBadge } from "@/shared/components/ui";
import { SERVICE_STATUSES, serviceTone, useServiceBookings, useUpdateBookingStatus, type ServiceBooking } from "@/features/service/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateTime } from "@/shared/format";
import { ApiError } from "@/core/api/client";

export default function Service() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useServiceBookings();
  const update = useUpdateBookingStatus();
  const [picking, setPicking] = useState<ServiceBooking | null>(null);
  const rows = data?.pages.flatMap((p) => p.content) ?? [];

  const choose = (status: string) => {
    if (!picking) return;
    update.mutate({ id: picking.id, status }, { onSuccess: () => setPicking(null) });
  };

  if (isLoading) return <Screen><LoadingView label="Loading bookings…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  return (
    <Screen padded={false}>
      <FlatList
        data={rows}
        keyExtractor={(b) => b.id}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        onEndReachedThreshold={0.4}
        onEndReached={() => {
          if (hasNextPage && !isFetchingNextPage) void fetchNextPage();
        }}
        ListEmptyComponent={<EmptyState emoji="🔧" title="No service bookings" hint="Requests from the store appear here." />}
        ListFooterComponent={isFetchingNextPage ? <View style={{ paddingVertical: 16 }}><LoadingView label="Loading more…" /></View> : null}
        renderItem={({ item }) => (
          <Pressable onPress={() => setPicking(item)} style={({ pressed }) => ({ opacity: pressed ? 0.7 : 1 })}>
            <Card style={{ padding: 14 }}>
              <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                <AppText weight="700" size={14} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>{item.customerName}</AppText>
                <StatusBadge label={item.bookingStatus} tone={serviceTone(item.bookingStatus)} />
              </View>
              <AppText size={13} style={{ marginTop: 4 }}>{item.serviceType}</AppText>
              <AppText muted size={12} style={{ marginTop: 4 }}>
                {item.phone}{item.preferredDate ? ` · prefers ${item.preferredDate}` : ""} · {dateTime(item.createdAt)}
              </AppText>
              {item.message ? <AppText muted size={12} style={{ marginTop: 4 }} numberOfLines={2}>{item.message}</AppText> : null}
            </Card>
          </Pressable>
        )}
      />

      <Modal visible={!!picking} transparent animationType="slide" onRequestClose={() => setPicking(null)}>
        <View style={{ flex: 1, backgroundColor: "#0006", justifyContent: "flex-end" }}>
          <View style={{ backgroundColor: t.bg, borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20 }}>
            <AppText weight="800" size={16} style={{ marginBottom: 12 }}>Set booking status</AppText>
            <View style={{ gap: 8 }}>
              {SERVICE_STATUSES.map((s) => (
                <Button key={s} title={s} variant={s === picking?.bookingStatus ? "primary" : "outline"} loading={update.isPending && update.variables?.status === s} onPress={() => choose(s)} />
              ))}
            </View>
            <View style={{ height: 12 }} />
            <Button title="Cancel" variant="outline" onPress={() => setPicking(null)} />
          </View>
        </View>
      </Modal>
    </Screen>
  );
}
