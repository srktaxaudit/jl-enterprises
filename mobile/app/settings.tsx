import { useState } from "react";
import { FlatList, Modal, Pressable, RefreshControl, View } from "react-native";
import { AppText, Button, Card, EmptyState, ErrorView, LoadingView, Screen, TextField } from "@/shared/components/ui";
import { useSettings, useUpsertSetting, type Setting } from "@/features/settings/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { ApiError } from "@/core/api/client";

export default function Settings() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching } = useSettings();
  const upsert = useUpsertSetting();

  const [editing, setEditing] = useState<Setting | null>(null);
  const [value, setValue] = useState("");

  const open = (s: Setting) => { setEditing(s); setValue(s.value ?? ""); };
  const save = () => { if (editing) upsert.mutate({ key: editing.key, value }, { onSuccess: () => setEditing(null) }); };

  if (isLoading) return <Screen><LoadingView label="Loading settings…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  return (
    <Screen padded={false}>
      <FlatList
        data={data ?? []}
        keyExtractor={(s) => s.key}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        ListHeaderComponent={<AppText muted size={13} style={{ marginBottom: 6 }}>Tap a setting to edit its value.</AppText>}
        ListEmptyComponent={<EmptyState emoji="⚙️" title="No settings" />}
        renderItem={({ item }) => (
          <Pressable onPress={() => open(item)} style={({ pressed }) => ({ opacity: pressed ? 0.7 : 1 })}>
            <Card style={{ padding: 14 }}>
              <AppText weight="700" size={13}>{item.key}</AppText>
              <AppText muted size={13} style={{ marginTop: 4 }} numberOfLines={2}>{item.value || "—"}</AppText>
            </Card>
          </Pressable>
        )}
      />

      <Modal visible={!!editing} transparent animationType="slide" onRequestClose={() => setEditing(null)}>
        <View style={{ flex: 1, backgroundColor: "#0006", justifyContent: "flex-end" }}>
          <View style={{ backgroundColor: t.bg, borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20 }}>
            <AppText weight="800" size={16} style={{ marginBottom: 12 }}>{editing?.key}</AppText>
            <TextField label="Value" value={value} onChangeText={setValue} multiline />
            {upsert.isError ? (
              <AppText color={t.danger} size={13} style={{ marginBottom: 10 }}>
                {upsert.error instanceof ApiError ? upsert.error.message : "Could not save"}
              </AppText>
            ) : null}
            <Button title="Save" onPress={save} loading={upsert.isPending} />
            <View style={{ height: 10 }} />
            <Button title="Cancel" variant="outline" onPress={() => setEditing(null)} />
          </View>
        </View>
      </Modal>
    </Screen>
  );
}
