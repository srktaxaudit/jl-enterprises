import { FlatList, RefreshControl, View } from "react-native";
import { AppText, Card, EmptyState, ErrorView, LoadingView, Screen, StatusBadge } from "@/shared/components/ui";
import { useAccounts } from "@/features/accounting/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

export default function Accounts() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching } = useAccounts();

  if (isLoading) return <Screen><LoadingView label="Loading accounts…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  return (
    <Screen padded={false}>
      <FlatList
        data={data ?? []}
        keyExtractor={(a) => a.id}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        ListEmptyComponent={<EmptyState emoji="📒" title="No accounts" />}
        renderItem={({ item }) => (
          <Card style={{ padding: 14 }}>
            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
              <AppText weight="700" size={14} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>
                {item.code} · {item.name}
              </AppText>
              <AppText weight="700" size={13}>{inr(item.openingBalance)}</AppText>
            </View>
            <View style={{ flexDirection: "row", gap: 8, marginTop: 8, alignItems: "center" }}>
              <StatusBadge label={item.accountGroup} tone="info" />
              {!item.active ? <StatusBadge label="Inactive" tone="muted" /> : null}
              {item.blocked ? <StatusBadge label="Blocked" tone="danger" /> : null}
              {item.systemAccount ? <StatusBadge label="System" tone="muted" /> : null}
            </View>
          </Card>
        )}
      />
    </Screen>
  );
}
