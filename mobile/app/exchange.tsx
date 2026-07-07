import { useState } from "react";
import { FlatList, Modal, Pressable, RefreshControl, View } from "react-native";
import { AppText, Button, Card, EmptyState, ErrorView, LoadingView, Screen, StatusBadge } from "@/shared/components/ui";
import { EXCHANGE_STATUSES, exchangeTone, useExchanges, useUpdateExchangeStatus, type ExchangeRequest } from "@/features/exchange/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateOnly, inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

export default function Exchange() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useExchanges();
  const update = useUpdateExchangeStatus();
  const [picking, setPicking] = useState<ExchangeRequest | null>(null);
  const rows = data?.pages.flatMap((p) => p.content) ?? [];

  const choose = (status: string) => {
    if (!picking) return;
    update.mutate({ id: picking.id, status }, { onSuccess: () => setPicking(null) });
  };

  if (isLoading) return <Screen><LoadingView label="Loading exchange requests…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  return (
    <Screen padded={false}>
      <FlatList
        data={rows}
        keyExtractor={(e) => e.id}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        onEndReachedThreshold={0.4}
        onEndReached={() => {
          if (hasNextPage && !isFetchingNextPage) void fetchNextPage();
        }}
        ListEmptyComponent={<EmptyState emoji="♻️" title="No exchange requests" hint="Trade-in requests appear here." />}
        ListFooterComponent={isFetchingNextPage ? <View style={{ paddingVertical: 16 }}><LoadingView label="Loading more…" /></View> : null}
        renderItem={({ item }) => {
          const value = item.finalValue ?? item.estimatedValue;
          return (
            <Pressable onPress={() => setPicking(item)} style={({ pressed }) => ({ opacity: pressed ? 0.7 : 1 })}>
              <Card style={{ padding: 14 }}>
                <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                  <AppText weight="700" size={14} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>
                    {item.applianceCategory}{item.brand ? ` · ${item.brand}` : ""}
                  </AppText>
                  <StatusBadge label={item.status.replace(/_/g, " ")} tone={exchangeTone(item.status)} />
                </View>
                <AppText muted size={12} style={{ marginTop: 4 }} numberOfLines={1}>
                  {item.customerName}{item.conditionGrade ? ` · ${item.conditionGrade}` : ""}{item.working ? "" : " · not working"}
                </AppText>
                <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginTop: 8 }}>
                  <AppText muted size={12}>
                    {item.finalValue != null ? "Approved" : "Estimated"} value
                  </AppText>
                  <AppText weight="800" size={15}>{value != null ? inr(value) : "—"}</AppText>
                </View>
                <AppText muted size={11} style={{ marginTop: 4 }}>Submitted {dateOnly(item.createdAt)}</AppText>
              </Card>
            </Pressable>
          );
        }}
      />

      <Modal visible={!!picking} transparent animationType="slide" onRequestClose={() => setPicking(null)}>
        <View style={{ flex: 1, backgroundColor: "#0006", justifyContent: "flex-end" }}>
          <View style={{ backgroundColor: t.bg, borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20 }}>
            <AppText weight="800" size={16} style={{ marginBottom: 4 }}>Update request status</AppText>
            <AppText muted size={12} style={{ marginBottom: 12 }}>
              Set the final value on the web admin before approving so it can be applied at checkout.
            </AppText>
            <View style={{ gap: 8 }}>
              {EXCHANGE_STATUSES.map((s) => (
                <Button key={s} title={s.replace(/_/g, " ")} variant="outline" loading={update.isPending && update.variables?.status === s} onPress={() => choose(s)} />
              ))}
            </View>
            {update.isError ? (
              <AppText color={t.danger} size={13} style={{ marginTop: 10 }}>
                {update.error instanceof ApiError ? update.error.message : "Could not update"}
              </AppText>
            ) : null}
            <View style={{ height: 12 }} />
            <Button title="Cancel" variant="outline" onPress={() => setPicking(null)} />
          </View>
        </View>
      </Modal>
    </Screen>
  );
}
