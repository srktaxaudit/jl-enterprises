import { FlatList, RefreshControl, View } from "react-native";
import { AppText, Card, EmptyState, ErrorView, LoadingView, Screen, StatusBadge } from "@/shared/components/ui";
import { couponState, couponValue, useCoupons, type Coupon } from "@/features/offers/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateOnly, inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

function CouponRow({ c }: { c: Coupon }) {
  const state = couponState(c);
  return (
    <Card style={{ padding: 14 }}>
      <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
        <AppText weight="800" size={15}>{c.code}</AppText>
        <StatusBadge label={state.label} tone={state.tone} />
      </View>
      <AppText size={13} style={{ marginTop: 4 }}>{couponValue(c)}{c.name ? ` · ${c.name}` : ""}</AppText>
      <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 12, marginTop: 8 }}>
        {c.minOrderAmount ? <AppText muted size={12}>Min {inr(c.minOrderAmount)}</AppText> : null}
        {c.maxDiscount ? <AppText muted size={12}>Cap {inr(c.maxDiscount)}</AppText> : null}
        <AppText muted size={12}>Used {c.usedCount}{c.usageLimit ? ` / ${c.usageLimit}` : ""}</AppText>
        {c.expiresAt ? <AppText muted size={12}>Ends {dateOnly(c.expiresAt)}</AppText> : null}
        {c.firstOrderOnly ? <AppText muted size={12}>First order only</AppText> : null}
      </View>
    </Card>
  );
}

export default function Offers() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching } = useCoupons();

  if (isLoading) return <Screen><LoadingView label="Loading offers…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  return (
    <Screen padded={false}>
      <FlatList
        data={data ?? []}
        keyExtractor={(c) => c.id}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        ListHeaderComponent={<AppText muted size={13} style={{ marginBottom: 6 }}>Create or edit coupons from the web admin. This is a live overview.</AppText>}
        ListEmptyComponent={<EmptyState emoji="🏷️" title="No coupons yet" hint="Add offers from the web admin." />}
        renderItem={({ item }) => <CouponRow c={item} />}
      />
    </Screen>
  );
}
