import React from "react";
import { Pressable, StyleSheet, View } from "react-native";
import { Image } from "expo-image";
import { useRouter } from "expo-router";
import { AppText, RatingStars } from "./ui";
import { useTheme } from "@/core/theme/ThemeProvider";
import { inr } from "@/shared/format";
import type { ProductSummary } from "@/core/types";

/** Grid card used on Home (featured) and Shop (search results). */
export function ProductCard({ product }: { product: ProductSummary }) {
  const t = useTheme();
  const router = useRouter();
  const outOfStock = product.availableStock != null && product.availableStock <= 0;
  const hasDiscount = product.comparePrice != null && Number(product.comparePrice) > Number(product.price);

  return (
    <Pressable
      onPress={() => router.push(`/product/${product.slug}`)}
      style={({ pressed }) => [
        styles.card,
        { backgroundColor: t.surface, borderColor: t.border, opacity: pressed ? 0.9 : 1 },
      ]}
      accessibilityRole="button"
      accessibilityLabel={product.name}
    >
      <View style={[styles.imageWrap, { backgroundColor: t.surfaceAlt }]}>
        {product.primaryImageUrl ? (
          <Image source={{ uri: product.primaryImageUrl }} style={styles.image} contentFit="contain" transition={150} />
        ) : (
          <AppText muted size={24}>🛋️</AppText>
        )}
        {hasDiscount && product.discountPercent ? (
          <View style={[styles.discount, { backgroundColor: t.accent }]}>
            <AppText color={t.onPrimary} size={10} weight="800">{Math.round(Number(product.discountPercent))}% OFF</AppText>
          </View>
        ) : null}
      </View>
      <View style={{ padding: 10, gap: 4 }}>
        {product.brandName ? <AppText muted size={11} weight="600" numberOfLines={1}>{product.brandName}</AppText> : null}
        <AppText weight="600" size={13} numberOfLines={2} style={{ minHeight: 34 }}>{product.name}</AppText>
        <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
          <AppText weight="800" size={15}>{inr(product.price)}</AppText>
          {hasDiscount ? (
            <AppText muted size={12} style={{ textDecorationLine: "line-through" }}>{inr(product.comparePrice)}</AppText>
          ) : null}
        </View>
        {product.reviewCount > 0 ? (
          <View style={{ flexDirection: "row", alignItems: "center", gap: 4 }}>
            <RatingStars rating={product.averageRating} size={11} />
            <AppText muted size={11}>({product.reviewCount})</AppText>
          </View>
        ) : null}
        {product.emiAvailable && product.emiAmount ? (
          <AppText size={11} color={t.success} weight="600">EMI from {inr(product.emiAmount)}/mo</AppText>
        ) : null}
        {outOfStock ? <AppText size={11} color={t.danger} weight="700">Out of stock</AppText> : null}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: { flex: 1, borderWidth: 1, borderRadius: 16, overflow: "hidden", margin: 6 },
  imageWrap: { height: 140, alignItems: "center", justifyContent: "center" },
  image: { width: "100%", height: "100%" },
  discount: { position: "absolute", top: 8, left: 8, borderRadius: 6, paddingHorizontal: 6, paddingVertical: 2 },
});
