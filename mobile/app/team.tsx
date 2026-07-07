import { FlatList, RefreshControl, View } from "react-native";
import { AppText, Card, EmptyState, ErrorView, LoadingView, Screen, StatusBadge } from "@/shared/components/ui";
import { useRoles } from "@/features/team/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { ApiError } from "@/core/api/client";

export default function Team() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching } = useRoles();

  if (isLoading) return <Screen><LoadingView label="Loading roles…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  return (
    <Screen padded={false}>
      <FlatList
        data={data ?? []}
        keyExtractor={(r) => r.id}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        ListHeaderComponent={<AppText muted size={13} style={{ marginBottom: 6 }}>Roles and their permissions. Assign roles to staff from the web admin.</AppText>}
        ListEmptyComponent={<EmptyState emoji="👤" title="No roles" />}
        renderItem={({ item }) => (
          <Card style={{ padding: 14 }}>
            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
              <AppText weight="700" size={14}>{item.name.replace(/^ROLE_/, "")}</AppText>
              <StatusBadge label={`${item.permissions?.length ?? 0} perms`} tone="info" />
            </View>
            {item.description ? <AppText muted size={12} style={{ marginTop: 4 }}>{item.description}</AppText> : null}
          </Card>
        )}
      />
    </Screen>
  );
}
