import { useState } from "react";
import { FlatList, Modal, RefreshControl, View } from "react-native";
import {
  AppText,
  Button,
  Card,
  EmptyState,
  ErrorView,
  LoadingView,
  Screen,
  StatusBadge,
  TextField,
} from "@/shared/components/ui";
import { stockTone, useLowStock, useUpdateInventory, type InventoryItem } from "@/features/inventory/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { ApiError } from "@/core/api/client";

export default function Inventory() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching } = useLowStock();
  const update = useUpdateInventory();

  const [editing, setEditing] = useState<InventoryItem | null>(null);
  const [qty, setQty] = useState("");
  const [reorder, setReorder] = useState("");
  const [loc, setLoc] = useState("");

  const openEdit = (i: InventoryItem) => {
    setEditing(i);
    setQty(String(i.quantity));
    setReorder(String(i.reorderLevel));
    setLoc(i.warehouseLocation ?? "");
  };
  const save = () => {
    if (!editing) return;
    update.mutate(
      { productId: editing.productId, quantity: Number(qty) || 0, reorderLevel: Number(reorder) || 0, warehouseLocation: loc.trim() },
      { onSuccess: () => setEditing(null) },
    );
  };

  if (isLoading) return <Screen><LoadingView label="Loading stock…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  return (
    <Screen padded={false}>
      <FlatList
        data={data ?? []}
        keyExtractor={(i) => i.productId}
        contentContainerStyle={{ padding: 16, gap: 10, flexGrow: 1 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
        ListHeaderComponent={
          <AppText muted size={13} style={{ marginBottom: 6 }}>
            Products at or below their reorder level. Tap one to restock.
          </AppText>
        }
        ListEmptyComponent={<EmptyState emoji="✅" title="All stocked up" hint="Nothing is at or below its reorder level." />}
        renderItem={({ item }) => (
          <Card style={{ padding: 14 }}>
            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
              <AppText weight="700" size={14} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>{item.productName}</AppText>
              <StatusBadge label={item.stockStatus.replace(/_/g, " ")} tone={stockTone(item.stockStatus)} />
            </View>
            <View style={{ flexDirection: "row", justifyContent: "space-between", marginTop: 10 }}>
              <AppText muted size={12}>Available {item.available} · On hand {item.quantity} · Reorder ≤ {item.reorderLevel}</AppText>
            </View>
            <View style={{ marginTop: 12 }}>
              <Button title="Restock / edit" variant="outline" onPress={() => openEdit(item)} />
            </View>
          </Card>
        )}
      />

      <Modal visible={!!editing} transparent animationType="slide" onRequestClose={() => setEditing(null)}>
        <View style={{ flex: 1, backgroundColor: "#0006", justifyContent: "flex-end" }}>
          <View style={{ backgroundColor: t.bg, borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20 }}>
            <AppText weight="800" size={17} style={{ marginBottom: 4 }}>Update stock</AppText>
            <AppText muted size={13} style={{ marginBottom: 16 }} numberOfLines={1}>{editing?.productName}</AppText>
            <TextField label="Quantity on hand" value={qty} onChangeText={setQty} keyboardType="number-pad" />
            <TextField label="Reorder level" value={reorder} onChangeText={setReorder} keyboardType="number-pad" />
            <TextField label="Warehouse location" value={loc} onChangeText={setLoc} autoCapitalize="characters" />
            {update.isError ? (
              <AppText color={t.danger} size={13} style={{ marginBottom: 10 }}>
                {update.error instanceof ApiError ? update.error.message : "Could not save"}
              </AppText>
            ) : null}
            <Button title="Save" onPress={save} loading={update.isPending} />
            <View style={{ height: 10 }} />
            <Button title="Cancel" variant="outline" onPress={() => setEditing(null)} />
          </View>
        </View>
      </Modal>
    </Screen>
  );
}
