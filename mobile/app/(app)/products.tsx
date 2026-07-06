import { useEffect, useState } from "react";
import { FlatList, RefreshControl, View } from "react-native";
import { Image } from "expo-image";
import { AppText, Card, EmptyState, ErrorView, LoadingView, Screen, TextField } from "@/shared/components/ui";
import { useProducts, type ProductSummary } from "@/features/products/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

function ProductRow({ p }: { p: ProductSummary }) {
  const t = useTheme();
  return (
    <Card style={{ padding: 12, flexDirection: "row", gap: 12, alignItems: "center" }}>
      {p.primaryImageUrl ? (
        <Image source={{ uri: p.primaryImageUrl }} style={{ width: 56, height: 56, borderRadius: 10 }} contentFit="cover" transition={150} />
      ) : (
        <View style={{ width: 56, height: 56, borderRadius: 10, backgroundColor: t.surfaceAlt, alignItems: "center", justifyContent: "center" }}>
          <AppText size={22}>📦</AppText>
        </View>
      )}
      <View style={{ flex: 1 }}>
        <AppText weight="700" size={14} numberOfLines={1}>{p.name}</AppText>
        {p.brandName ? <AppText muted size={12}>{p.brandName}</AppText> : null}
      </View>
      <AppText weight="800" size={14}>{inr(p.price)}</AppText>
    </Card>
  );
}

export default function Products() {
  const t = useTheme();
  const [q, setQ] = useState("");
  const [debounced, setDebounced] = useState("");
  useEffect(() => {
    const id = setTimeout(() => setDebounced(q.trim()), 350);
    return () => clearTimeout(id);
  }, [q]);

  const { data, isLoading, isError, error, refetch, isRefetching, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useProducts(debounced || undefined);
  const rows = data?.pages.flatMap((p) => p.content) ?? [];

  return (
    <Screen padded={false}>
      <View style={{ padding: 16, paddingBottom: 0 }}>
        <TextField placeholder="Search products…" value={q} onChangeText={setQ} autoCapitalize="none" />
      </View>
      {isLoading ? (
        <LoadingView label="Loading products…" />
      ) : isError ? (
        <ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} />
      ) : (
        <FlatList
          data={rows}
          keyExtractor={(p) => p.id}
          contentContainerStyle={{ padding: 16, paddingTop: 4, gap: 10, flexGrow: 1 }}
          refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
          onEndReachedThreshold={0.4}
          onEndReached={() => {
            if (hasNextPage && !isFetchingNextPage) void fetchNextPage();
          }}
          ListEmptyComponent={<EmptyState title="No products" hint="Try a different search term." />}
          ListFooterComponent={isFetchingNextPage ? <View style={{ paddingVertical: 16 }}><LoadingView label="Loading more…" /></View> : null}
          renderItem={({ item }) => <ProductRow p={item} />}
        />
      )}
    </Screen>
  );
}
