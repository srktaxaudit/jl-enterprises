import React from "react";
import { FlatList, Pressable, RefreshControl, ScrollView, StyleSheet, View } from "react-native";
import { Image } from "expo-image";
import { useRouter } from "expo-router";
import { AppText, Screen } from "@/shared/components/ui";
import { ProductCard } from "@/shared/components/ProductCard";
import { useTheme } from "@/core/theme/ThemeProvider";
import { useBanners, useCategories, useFeaturedProducts } from "@/features/catalog/hooks";

export default function Home() {
  const t = useTheme();
  const router = useRouter();
  const banners = useBanners();
  const categories = useCategories();
  const featured = useFeaturedProducts();

  const refreshing = banners.isRefetching || categories.isRefetching || featured.isRefetching;
  const onRefresh = () => {
    void banners.refetch();
    void categories.refetch();
    void featured.refetch();
  };

  const activeBanners = (banners.data ?? []).filter((b) => b.active && b.imageUrl);

  return (
    <Screen padded={false}>
      <ScrollView
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={t.accent} />}
        contentContainerStyle={{ paddingBottom: 24 }}
      >
        {activeBanners.length > 0 ? (
          <FlatList
            horizontal
            pagingEnabled
            showsHorizontalScrollIndicator={false}
            data={activeBanners}
            keyExtractor={(b) => b.id}
            style={{ marginTop: 12 }}
            renderItem={({ item }) => (
              <View style={[styles.banner, { backgroundColor: t.surfaceAlt }]}>
                <Image source={{ uri: item.imageUrl }} style={{ width: "100%", height: "100%" }} contentFit="cover" transition={150} />
              </View>
            )}
          />
        ) : null}

        <View style={styles.sectionHead}>
          <AppText weight="800" size={17}>Shop by category</AppText>
        </View>
        <FlatList
          horizontal
          showsHorizontalScrollIndicator={false}
          data={categories.data ?? []}
          keyExtractor={(c) => c.id}
          contentContainerStyle={{ paddingHorizontal: 10 }}
          renderItem={({ item }) => (
            <Pressable
              onPress={() => router.push({ pathname: "/(tabs)/shop", params: { category: item.slug } })}
              style={({ pressed }) => [
                styles.categoryChip,
                { backgroundColor: t.surface, borderColor: t.border, opacity: pressed ? 0.8 : 1 },
              ]}
            >
              {item.imageUrl ? (
                <Image source={{ uri: item.imageUrl }} style={styles.categoryImage} contentFit="cover" transition={150} />
              ) : (
                <View style={[styles.categoryImage, { backgroundColor: t.surfaceAlt, alignItems: "center", justifyContent: "center" }]}>
                  <AppText size={20}>🛍️</AppText>
                </View>
              )}
              <AppText size={12} weight="600" numberOfLines={1} style={{ maxWidth: 84, textAlign: "center" }}>
                {item.name}
              </AppText>
            </Pressable>
          )}
        />

        <View style={styles.sectionHead}>
          <AppText weight="800" size={17}>Featured products</AppText>
          <Pressable onPress={() => router.push("/(tabs)/shop")} hitSlop={8}>
            <AppText size={13} weight="700" color={t.accent}>See all</AppText>
          </Pressable>
        </View>
        <View style={styles.grid}>
          {(featured.data ?? []).map((p) => (
            <View key={p.id} style={{ width: "50%" }}>
              <ProductCard product={p} />
            </View>
          ))}
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  banner: { width: 340, height: 150, borderRadius: 16, overflow: "hidden", marginHorizontal: 8 },
  sectionHead: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 16,
    marginTop: 20,
    marginBottom: 10,
  },
  categoryChip: {
    alignItems: "center",
    gap: 6,
    borderWidth: 1,
    borderRadius: 14,
    padding: 10,
    marginHorizontal: 5,
  },
  categoryImage: { width: 56, height: 56, borderRadius: 12 },
  grid: { flexDirection: "row", flexWrap: "wrap", paddingHorizontal: 10 },
});
