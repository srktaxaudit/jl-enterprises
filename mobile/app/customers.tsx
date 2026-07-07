import { useEffect, useState } from "react";
import { Alert, FlatList, Pressable, RefreshControl, View } from "react-native";
import { AppText, Card, EmptyState, ErrorView, LoadingView, Screen, StatusBadge, TextField } from "@/shared/components/ui";
import { bareRoles, fullName, useCustomers, useSetUserEnabled, type UserRow } from "@/features/customers/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { ApiError } from "@/core/api/client";

export default function Customers() {
  const t = useTheme();
  const [q, setQ] = useState("");
  const [debounced, setDebounced] = useState("");
  useEffect(() => {
    const id = setTimeout(() => setDebounced(q.trim()), 350);
    return () => clearTimeout(id);
  }, [q]);

  const { data, isLoading, isError, error, refetch, isRefetching, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useCustomers(debounced || undefined);
  const setEnabled = useSetUserEnabled();
  const rows = data?.pages.flatMap((p) => p.content) ?? [];

  const toggle = (u: UserRow) => {
    Alert.alert(
      u.enabled ? "Disable user?" : "Enable user?",
      `${fullName(u)} (${u.email})`,
      [
        { text: "Cancel", style: "cancel" },
        {
          text: u.enabled ? "Disable" : "Enable",
          style: u.enabled ? "destructive" : "default",
          onPress: () => setEnabled.mutate({ id: u.id, enabled: !u.enabled }),
        },
      ],
    );
  };

  return (
    <Screen padded={false}>
      <View style={{ padding: 16, paddingBottom: 0 }}>
        <TextField placeholder="Search name, email or phone…" value={q} onChangeText={setQ} autoCapitalize="none" />
      </View>
      {isLoading ? (
        <LoadingView label="Loading customers…" />
      ) : isError ? (
        <ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} />
      ) : (
        <FlatList
          data={rows}
          keyExtractor={(u) => u.id}
          contentContainerStyle={{ padding: 16, paddingTop: 4, gap: 10, flexGrow: 1 }}
          refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
          onEndReachedThreshold={0.4}
          onEndReached={() => {
            if (hasNextPage && !isFetchingNextPage) void fetchNextPage();
          }}
          ListEmptyComponent={<EmptyState emoji="👥" title="No customers" hint="Try a different search." />}
          ListFooterComponent={isFetchingNextPage ? <View style={{ paddingVertical: 16 }}><LoadingView label="Loading more…" /></View> : null}
          renderItem={({ item }) => (
            <Pressable onPress={() => toggle(item)} style={({ pressed }) => ({ opacity: pressed ? 0.7 : 1 })}>
              <Card style={{ padding: 14 }}>
                <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                  <AppText weight="700" size={14} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>{fullName(item)}</AppText>
                  <StatusBadge label={item.enabled ? "Active" : "Disabled"} tone={item.enabled ? "success" : "muted"} />
                </View>
                <AppText muted size={12} style={{ marginTop: 4 }} numberOfLines={1}>{item.email}{item.phone ? ` · ${item.phone}` : ""}</AppText>
                {bareRoles(item) ? <AppText muted size={12} style={{ marginTop: 2 }}>{bareRoles(item)}</AppText> : null}
              </Card>
            </Pressable>
          )}
        />
      )}
    </Screen>
  );
}
