import { RefreshControl, ScrollView, useWindowDimensions, View } from "react-native";
import { AppText, Card, ErrorView, LoadingView, Screen } from "@/shared/components/ui";
import { useDashboardStats } from "@/features/dashboard/hooks";
import { useAuth } from "@/core/auth/authStore";
import { displayName } from "@/core/auth/rbac";
import { useTheme } from "@/core/theme/ThemeProvider";
import { inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

function Stat({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <Card style={{ padding: 14 }}>
      <AppText muted size={11} weight="600">{label.toUpperCase()}</AppText>
      <AppText weight="800" size={22} style={{ marginTop: 4 }}>{value}</AppText>
      {sub ? <AppText muted size={12} style={{ marginTop: 2 }}>{sub}</AppText> : null}
    </Card>
  );
}

export default function Dashboard() {
  const t = useTheme();
  const user = useAuth((s) => s.user);
  const { width } = useWindowDimensions();
  const cols = width >= 700 ? 3 : 2;
  const { data, isLoading, isError, error, refetch, isRefetching } = useDashboardStats();

  const hour = new Date().getHours();
  const greet = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";

  if (isLoading) return <Screen><LoadingView label="Loading dashboard…" /></Screen>;
  if (isError || !data) {
    return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;
  }

  const s = data;
  const cards: Array<[string, string, string?]> = [
    ["Revenue (30d)", inr(s.revenueLast30Days), `${s.ordersLast30Days} orders`],
    ["Orders", String(s.totalOrders), "all time"],
    ["Pending", String(s.pendingOrders), "to fulfil"],
    ["Delivered", String(s.deliveredOrders ?? 0), "completed"],
    ["Products", String(s.totalProducts), `${s.inStockCount ?? 0} in stock`],
    ["Low stock", String(s.lowStockCount), "need restock"],
    ["Out of stock", String(s.outOfStockCount ?? 0), "unavailable"],
    ["Customers", String(s.totalUsers), "total users"],
  ];

  return (
    <Screen padded={false}>
      <ScrollView
        contentContainerStyle={{ padding: 16 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
      >
        <AppText weight="800" size={20}>{greet}, {displayName(user)} 👋</AppText>
        <AppText muted size={13} style={{ marginBottom: 16 }}>Here&apos;s what&apos;s happening at JL Enterprises today.</AppText>
        <View style={{ flexDirection: "row", flexWrap: "wrap", marginHorizontal: -6 }}>
          {cards.map(([label, value, sub]) => (
            <View key={label} style={{ width: `${100 / cols}%`, padding: 6 }}>
              <Stat label={label} value={value} sub={sub} />
            </View>
          ))}
        </View>
      </ScrollView>
    </Screen>
  );
}
