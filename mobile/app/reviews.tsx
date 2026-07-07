import { useState } from "react";
import { FlatList, Pressable, RefreshControl, View } from "react-native";
import { AppText, Button, Card, EmptyState, ErrorView, LoadingView, Screen, StatusBadge } from "@/shared/components/ui";
import { REVIEW_TABS, reviewTone, useModerateReview, useReviews, type Review } from "@/features/reviews/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateOnly } from "@/shared/format";
import { ApiError } from "@/core/api/client";

function Stars({ n }: { n: number }) {
  return <AppText size={13}>{"★".repeat(Math.max(0, Math.min(5, n)))}{"☆".repeat(Math.max(0, 5 - n))}</AppText>;
}

export default function Reviews() {
  const t = useTheme();
  const [status, setStatus] = useState<string>("PENDING");
  const { data, isLoading, isError, error, refetch, isRefetching, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useReviews(status);
  const moderate = useModerateReview();
  const rows = data?.pages.flatMap((p) => p.content) ?? [];

  return (
    <Screen padded={false}>
      <View style={{ flexDirection: "row", gap: 8, padding: 16, paddingBottom: 8 }}>
        {REVIEW_TABS.map((s) => {
          const active = s === status;
          return (
            <Pressable
              key={s}
              onPress={() => setStatus(s)}
              style={{
                flex: 1,
                paddingVertical: 9,
                borderRadius: 10,
                alignItems: "center",
                backgroundColor: active ? t.primary : t.surface,
                borderWidth: 1,
                borderColor: active ? t.primary : t.border,
              }}
            >
              <AppText size={12} weight="700" color={active ? t.onPrimary : t.textMuted}>{s}</AppText>
            </Pressable>
          );
        })}
      </View>

      {isLoading ? (
        <LoadingView label="Loading reviews…" />
      ) : isError ? (
        <ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} />
      ) : (
        <FlatList
          data={rows}
          keyExtractor={(r) => r.id}
          contentContainerStyle={{ padding: 16, paddingTop: 4, gap: 10, flexGrow: 1 }}
          refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
          onEndReachedThreshold={0.4}
          onEndReached={() => {
            if (hasNextPage && !isFetchingNextPage) void fetchNextPage();
          }}
          ListEmptyComponent={<EmptyState emoji="⭐" title={`No ${status.toLowerCase()} reviews`} />}
          ListFooterComponent={isFetchingNextPage ? <View style={{ paddingVertical: 16 }}><LoadingView label="Loading more…" /></View> : null}
          renderItem={({ item }: { item: Review }) => (
            <Card style={{ padding: 14 }}>
              <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                <Stars n={item.rating} />
                <StatusBadge label={item.status} tone={reviewTone(item.status)} />
              </View>
              {item.title ? <AppText weight="700" size={14} style={{ marginTop: 6 }}>{item.title}</AppText> : null}
              {item.comment ? <AppText size={13} style={{ marginTop: 2 }}>{item.comment}</AppText> : null}
              <AppText muted size={12} style={{ marginTop: 6 }}>
                {item.reviewerName}{item.verifiedPurchase ? " · verified" : ""} · {dateOnly(item.createdAt)}
              </AppText>
              {status === "PENDING" ? (
                <View style={{ flexDirection: "row", gap: 8, marginTop: 12 }}>
                  <View style={{ flex: 1 }}>
                    <Button
                      title="Approve"
                      variant="accent"
                      loading={moderate.isPending && moderate.variables?.id === item.id && moderate.variables?.status === "APPROVED"}
                      onPress={() => moderate.mutate({ id: item.id, status: "APPROVED" })}
                    />
                  </View>
                  <View style={{ flex: 1 }}>
                    <Button
                      title="Reject"
                      variant="danger"
                      loading={moderate.isPending && moderate.variables?.id === item.id && moderate.variables?.status === "REJECTED"}
                      onPress={() => moderate.mutate({ id: item.id, status: "REJECTED" })}
                    />
                  </View>
                </View>
              ) : null}
            </Card>
          )}
        />
      )}
    </Screen>
  );
}
