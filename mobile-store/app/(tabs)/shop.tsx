import React, { useEffect, useMemo, useState } from "react";
import { FlatList, Pressable, StyleSheet, View } from "react-native";
import { useLocalSearchParams } from "expo-router";
import { AppText, EmptyState, ErrorView, LoadingView, Screen, TextField } from "@/shared/components/ui";
import { ProductCard } from "@/shared/components/ProductCard";
import { useTheme } from "@/core/theme/ThemeProvider";
import { useCategories, useProductSearch } from "@/features/catalog/hooks";

const SORTS = [
  { label: "Newest", value: "" },
  { label: "Price ↑", value: "price,asc" },
  { label: "Price ↓", value: "price,desc" },
] as const;

export default function Shop() {
  const t = useTheme();
  const params = useLocalSearchParams<{ category?: string }>();
  const [search, setSearch] = useState("");
  const [debounced, setDebounced] = useState("");
  const [category, setCategory] = useState<string>("");
  const [sort, setSort] = useState<string>("");

  // A category tapped on the Home tab lands here via the route param.
  useEffect(() => {
    if (params.category) setCategory(params.category);
  }, [params.category]);

  useEffect(() => {
    const id = setTimeout(() => setDebounced(search.trim()), 350);
    return () => clearTimeout(id);
  }, [search]);

  const filters = useMemo(
    () => ({ search: debounced, category, sort }),
    [debounced, category, sort],
  );
  const query = useProductSearch(filters);
  const categories = useCategories();

  const products = (query.data?.pages ?? []).flatMap((p) => p.content);

  return (
    <Screen padded={false}>
      <View style={{ paddingHorizontal: 16, paddingTop: 12 }}>
        <TextField
          placeholder="Search appliances, furniture…"
          value={search}
          onChangeText={setSearch}
          autoCapitalize="none"
          returnKeyType="search"
        />
      </View>

      <View style={{ flexDirection: "row", paddingHorizontal: 12, marginBottom: 8, gap: 6 }}>
        {SORTS.map((s) => (
          <Chip key={s.label} label={s.label} active={sort === s.value} onPress={() => setSort(s.value)} />
        ))}
      </View>

      <FlatList
        horizontal
        showsHorizontalScrollIndicator={false}
        data={[{ id: "", name: "All", slug: "" }, ...(categories.data ?? [])]}
        keyExtractor={(c) => c.id || "all"}
        style={{ flexGrow: 0, marginBottom: 4 }}
        contentContainerStyle={{ paddingHorizontal: 12, gap: 6 }}
        renderItem={({ item }) => (
          <Chip label={item.name} active={category === item.slug} onPress={() => setCategory(item.slug)} />
        )}
      />

      {query.isLoading ? (
        <LoadingView label="Loading products…" />
      ) : query.isError ? (
        <ErrorView message={(query.error as Error).message} onRetry={() => query.refetch()} />
      ) : products.length === 0 ? (
        <EmptyState emoji="🔍" title="No products found" hint="Try a different search or category." />
      ) : (
        <FlatList
          data={products}
          numColumns={2}
          keyExtractor={(p) => p.id}
          contentContainerStyle={{ paddingHorizontal: 10, paddingBottom: 24 }}
          renderItem={({ item }) => (
            <View style={{ width: "50%" }}>
              <ProductCard product={item} />
            </View>
          )}
          onEndReachedThreshold={0.4}
          onEndReached={() => {
            if (query.hasNextPage && !query.isFetchingNextPage) void query.fetchNextPage();
          }}
          refreshing={query.isRefetching}
          onRefresh={() => void query.refetch()}
          ListFooterComponent={query.isFetchingNextPage ? <LoadingView label="Loading more…" /> : null}
        />
      )}
    </Screen>
  );
}

function Chip({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  const t = useTheme();
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.chip,
        {
          backgroundColor: active ? t.primary : t.surface,
          borderColor: active ? t.primary : t.border,
          opacity: pressed ? 0.8 : 1,
        },
      ]}
    >
      <AppText size={12} weight="700" color={active ? t.onPrimary : t.text}>{label}</AppText>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  chip: { borderWidth: 1, borderRadius: 999, paddingHorizontal: 12, paddingVertical: 6 },
});
