import { useEffect, useState } from "react";
import { FlatList, Modal, Pressable, RefreshControl, View } from "react-native";
import { AppText, Button, Card, EmptyState, ErrorView, LoadingView, Screen, StatusBadge, TextField } from "@/shared/components/ui";
import { useResetStaffPassword, useSetStaffEnabled, useStaff, type StaffRow } from "@/features/staff/hooks";
import { bareRoles, fullName } from "@/features/customers/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { ApiError } from "@/core/api/client";

export default function Staff() {
  const t = useTheme();
  const [q, setQ] = useState("");
  const [debounced, setDebounced] = useState("");
  useEffect(() => {
    const id = setTimeout(() => setDebounced(q.trim()), 350);
    return () => clearTimeout(id);
  }, [q]);

  const { data, isLoading, isError, error, refetch, isRefetching, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useStaff(debounced || undefined);
  const setEnabled = useSetStaffEnabled();
  const resetPw = useResetStaffPassword();

  const [selected, setSelected] = useState<StaffRow | null>(null);
  const [pw, setPw] = useState("");
  const [done, setDone] = useState<string | null>(null);

  const close = () => { setSelected(null); setPw(""); setDone(null); resetPw.reset(); };
  const rows = data?.pages.flatMap((p) => p.content) ?? [];

  return (
    <Screen padded={false}>
      <View style={{ padding: 16, paddingBottom: 0 }}>
        <TextField placeholder="Search staff by name or email…" value={q} onChangeText={setQ} autoCapitalize="none" />
      </View>
      {isLoading ? (
        <LoadingView label="Loading staff…" />
      ) : isError ? (
        <ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} />
      ) : (
        <FlatList
          data={rows}
          keyExtractor={(u) => u.id}
          contentContainerStyle={{ padding: 16, paddingTop: 4, gap: 10, flexGrow: 1 }}
          refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
          onEndReachedThreshold={0.4}
          onEndReached={() => { if (hasNextPage && !isFetchingNextPage) void fetchNextPage(); }}
          ListEmptyComponent={<EmptyState emoji="🧑‍💼" title="No staff" hint="Add staff accounts from the web admin." />}
          ListFooterComponent={isFetchingNextPage ? <View style={{ paddingVertical: 16 }}><LoadingView label="Loading more…" /></View> : null}
          renderItem={({ item }) => (
            <Pressable onPress={() => setSelected(item)} style={({ pressed }) => ({ opacity: pressed ? 0.7 : 1 })}>
              <Card style={{ padding: 14 }}>
                <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
                  <AppText weight="700" size={14} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>{fullName(item)}</AppText>
                  <StatusBadge label={item.enabled ? "Active" : "Disabled"} tone={item.enabled ? "success" : "muted"} />
                </View>
                <AppText muted size={12} style={{ marginTop: 4 }} numberOfLines={1}>{item.email}</AppText>
                {bareRoles(item) ? <AppText muted size={12} style={{ marginTop: 2 }}>{bareRoles(item)}</AppText> : null}
              </Card>
            </Pressable>
          )}
        />
      )}

      <Modal visible={!!selected} transparent animationType="slide" onRequestClose={close}>
        <View style={{ flex: 1, backgroundColor: "#0006", justifyContent: "flex-end" }}>
          <View style={{ backgroundColor: t.bg, borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20 }}>
            <AppText weight="800" size={16}>{selected ? fullName(selected) : ""}</AppText>
            <AppText muted size={12} style={{ marginBottom: 16 }} numberOfLines={1}>{selected?.email}</AppText>

            {done ? (
              <AppText color={t.success} size={13} style={{ marginBottom: 12 }}>{done}</AppText>
            ) : null}

            <Button
              title={selected?.enabled ? "Disable account" : "Enable account"}
              variant={selected?.enabled ? "danger" : "accent"}
              loading={setEnabled.isPending}
              onPress={() =>
                selected &&
                setEnabled.mutate(
                  { id: selected.id, enabled: !selected.enabled },
                  { onSuccess: () => setDone(selected.enabled ? "Account disabled." : "Account enabled.") },
                )
              }
            />

            <View style={{ height: 16 }} />
            <TextField label="New password" placeholder="At least 8 characters" value={pw} onChangeText={setPw} secureTextEntry autoCapitalize="none" />
            {resetPw.isError ? (
              <AppText color={t.danger} size={13} style={{ marginBottom: 10 }}>
                {resetPw.error instanceof ApiError ? resetPw.error.message : "Could not reset"}
              </AppText>
            ) : null}
            <Button
              title="Reset password"
              variant="outline"
              disabled={pw.trim().length < 8}
              loading={resetPw.isPending}
              onPress={() =>
                selected &&
                resetPw.mutate({ id: selected.id, password: pw.trim() }, { onSuccess: () => { setDone("Password reset."); setPw(""); } })
              }
            />

            <View style={{ height: 14 }} />
            <Button title="Close" variant="outline" onPress={close} />
          </View>
        </View>
      </Modal>
    </Screen>
  );
}
