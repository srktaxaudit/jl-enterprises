import React, { useMemo, useState } from "react";
import { Alert, FlatList, Pressable, ScrollView, StyleSheet, View, useWindowDimensions } from "react-native";
import { Image } from "expo-image";
import { Stack, useLocalSearchParams, useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import {
  AppText,
  Button,
  Card,
  ErrorView,
  LoadingView,
  QtyStepper,
  RatingStars,
  Screen,
} from "@/shared/components/ui";
import { ProductCard } from "@/shared/components/ProductCard";
import { useTheme } from "@/core/theme/ThemeProvider";
import { useAuth } from "@/core/auth/authStore";
import { useProduct, useProductReviews, useRelatedProducts, useSubmitReview } from "@/features/catalog/hooks";
import { useAddToCart } from "@/features/cart/hooks";
import { useToggleWishlist, useWishlist } from "@/features/wishlist/hooks";
import { ApiError } from "@/core/api/client";
import { dateOnly, inr } from "@/shared/format";

export default function ProductPage() {
  const t = useTheme();
  const router = useRouter();
  const { width } = useWindowDimensions();
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const authed = useAuth((s) => s.status === "authed");

  const product = useProduct(slug);
  const related = useRelatedProducts(slug);
  const reviews = useProductReviews(product.data?.id);
  const submitReview = useSubmitReview(product.data?.id);
  const addToCart = useAddToCart();
  const wishlist = useWishlist();
  const toggleWishlist = useToggleWishlist();

  const [qty, setQty] = useState(1);
  const [imageIndex, setImageIndex] = useState(0);
  const [myRating, setMyRating] = useState(0);

  const inWishlist = useMemo(
    () => !!wishlist.data?.items.some((i) => i.product.id === product.data?.id),
    [wishlist.data, product.data?.id],
  );

  if (product.isLoading) return <Screen><LoadingView label="Loading product…" /></Screen>;
  if (product.isError || !product.data) {
    return <Screen><ErrorView message={(product.error as Error)?.message ?? "Not found"} onRetry={() => product.refetch()} /></Screen>;
  }

  const p = product.data;
  const gallery = p.images.length > 0 ? p.images : p.primaryImageUrl ? [{ id: "primary", url: p.primaryImageUrl }] : [];
  const hasDiscount = p.comparePrice != null && Number(p.comparePrice) > Number(p.price);
  const outOfStock = p.availableStock != null && p.availableStock <= 0;

  const requireAuth = (action: string): boolean => {
    if (authed) return true;
    Alert.alert("Sign in required", `Please sign in to ${action}.`, [
      { text: "Cancel", style: "cancel" },
      { text: "Sign in", onPress: () => router.push("/login") },
    ]);
    return false;
  };

  const onAddToCart = () => {
    if (!requireAuth("add items to your cart")) return;
    addToCart.mutate(
      { productId: p.id, quantity: qty },
      {
        onSuccess: () => Alert.alert("Added to cart", `${p.name} × ${qty}`),
        onError: (e) => Alert.alert("Couldn't add to cart", e instanceof ApiError ? e.message : "Please try again."),
      },
    );
  };

  const onToggleWishlist = () => {
    if (!requireAuth("use your wishlist")) return;
    toggleWishlist.mutate({ productId: p.id, inWishlist });
  };

  const onSubmitReview = () => {
    if (!requireAuth("write a review")) return;
    if (myRating < 1) {
      Alert.alert("Pick a rating", "Tap the stars to rate this product first.");
      return;
    }
    submitReview.mutate(
      { rating: myRating },
      {
        onSuccess: () => {
          setMyRating(0);
          Alert.alert("Thanks!", "Your review was submitted and will appear after moderation.");
        },
        onError: (e) => Alert.alert("Couldn't submit review", e instanceof ApiError ? e.message : "Please try again."),
      },
    );
  };

  return (
    <Screen padded={false}>
      <Stack.Screen options={{ title: p.name }} />
      <ScrollView contentContainerStyle={{ paddingBottom: 24 }}>
        {/* Gallery */}
        <View style={{ backgroundColor: t.surface }}>
          <FlatList
            horizontal
            pagingEnabled
            showsHorizontalScrollIndicator={false}
            data={gallery}
            keyExtractor={(img) => img.id}
            onMomentumScrollEnd={(e) => setImageIndex(Math.round(e.nativeEvent.contentOffset.x / width))}
            renderItem={({ item }) => (
              <View style={{ width, height: 280, alignItems: "center", justifyContent: "center" }}>
                <Image source={{ uri: item.url }} style={{ width: "90%", height: "100%" }} contentFit="contain" transition={150} />
              </View>
            )}
            ListEmptyComponent={
              <View style={{ width, height: 280, alignItems: "center", justifyContent: "center" }}>
                <AppText size={48}>🛋️</AppText>
              </View>
            }
          />
          {gallery.length > 1 ? (
            <View style={styles.dots}>
              {gallery.map((img, i) => (
                <View
                  key={img.id}
                  style={[styles.dot, { backgroundColor: i === imageIndex ? t.accent : t.border }]}
                />
              ))}
            </View>
          ) : null}
          <Pressable onPress={onToggleWishlist} style={[styles.wishlistBtn, { backgroundColor: t.surfaceAlt }]} hitSlop={8}>
            <Ionicons name={inWishlist ? "heart" : "heart-outline"} size={22} color={inWishlist ? t.danger : t.textMuted} />
          </Pressable>
        </View>

        {/* Summary */}
        <View style={{ padding: 16, gap: 8 }}>
          {p.brandName ? <AppText muted weight="600" size={12}>{p.brandName}</AppText> : null}
          <AppText weight="800" size={19}>{p.name}</AppText>
          {p.reviewCount > 0 ? (
            <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
              <RatingStars rating={p.averageRating} />
              <AppText muted size={12}>{Number(p.averageRating ?? 0).toFixed(1)} · {p.reviewCount} review{p.reviewCount === 1 ? "" : "s"}</AppText>
            </View>
          ) : null}
          <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
            <AppText weight="800" size={22}>{inr(p.price)}</AppText>
            {hasDiscount ? (
              <>
                <AppText muted size={14} style={{ textDecorationLine: "line-through" }}>{inr(p.comparePrice)}</AppText>
                {p.discountPercent ? (
                  <AppText color={t.success} weight="800" size={14}>{Math.round(Number(p.discountPercent))}% off</AppText>
                ) : null}
              </>
            ) : null}
          </View>
          {outOfStock ? (
            <AppText color={t.danger} weight="700">Out of stock</AppText>
          ) : p.availableStock != null && p.availableStock <= 5 ? (
            <AppText color={t.warn} weight="700" size={13}>Only {p.availableStock} left</AppText>
          ) : null}
          {p.shortDescription ? <AppText muted size={13}>{p.shortDescription}</AppText> : null}
        </View>

        {/* EMI */}
        {p.emiAvailable && p.emiAmount ? (
          <Card style={{ marginHorizontal: 16, marginBottom: 12, gap: 4 }}>
            <AppText weight="700" size={14}>💳 EMI available</AppText>
            <AppText size={13}>
              {inr(p.emiAmount)}/month{p.emiMonths ? ` × ${p.emiMonths} months` : ""}
              {p.emiDownPayment ? ` · ${inr(p.emiDownPayment)} down` : ""}
            </AppText>
            {p.emiNote ? <AppText muted size={12}>{p.emiNote}</AppText> : null}
          </Card>
        ) : null}

        {/* Add to cart */}
        <Card style={{ marginHorizontal: 16, marginBottom: 12, gap: 12 }}>
          <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
            <AppText weight="600" size={14}>Quantity</AppText>
            <QtyStepper value={qty} onChange={setQty} />
          </View>
          <Button
            title={outOfStock ? "Out of stock" : "Add to cart"}
            variant="accent"
            disabled={outOfStock}
            loading={addToCart.isPending}
            onPress={onAddToCart}
          />
        </Card>

        {/* Description */}
        {p.description ? (
          <View style={{ paddingHorizontal: 16, marginBottom: 12, gap: 6 }}>
            <AppText weight="800" size={16}>About this product</AppText>
            <AppText muted size={13} style={{ lineHeight: 20 }}>{p.description}</AppText>
          </View>
        ) : null}

        {/* Reviews */}
        <View style={{ paddingHorizontal: 16, marginBottom: 12, gap: 10 }}>
          <AppText weight="800" size={16}>Reviews</AppText>
          {(reviews.data ?? []).length === 0 ? (
            <AppText muted size={13}>No reviews yet — be the first!</AppText>
          ) : (
            (reviews.data ?? []).map((r) => (
              <Card key={r.id} style={{ gap: 4, padding: 12 }}>
                <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
                  <RatingStars rating={r.rating} size={12} />
                  <AppText muted size={11}>{dateOnly(r.createdAt)}</AppText>
                </View>
                {r.title ? <AppText weight="700" size={13}>{r.title}</AppText> : null}
                {r.comment ? <AppText muted size={13}>{r.comment}</AppText> : null}
                <AppText muted size={11}>
                  {r.reviewerName ?? "Customer"}{r.verifiedPurchase ? " · ✅ Verified purchase" : ""}
                </AppText>
              </Card>
            ))
          )}
          <Card style={{ gap: 8, padding: 12 }}>
            <AppText weight="700" size={13}>Rate this product</AppText>
            <RatingStars rating={myRating} size={26} onRate={setMyRating} />
            <Button title="Submit review" variant="outline" loading={submitReview.isPending} onPress={onSubmitReview} />
          </Card>
        </View>

        {/* Related */}
        {(related.data ?? []).length > 0 ? (
          <View style={{ marginBottom: 12 }}>
            <AppText weight="800" size={16} style={{ paddingHorizontal: 16, marginBottom: 8 }}>You may also like</AppText>
            <FlatList
              horizontal
              showsHorizontalScrollIndicator={false}
              data={related.data}
              keyExtractor={(rp) => rp.id}
              contentContainerStyle={{ paddingHorizontal: 10 }}
              renderItem={({ item }) => (
                <View style={{ width: 170 }}>
                  <ProductCard product={item} />
                </View>
              )}
            />
          </View>
        ) : null}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  dots: { flexDirection: "row", justifyContent: "center", gap: 6, paddingVertical: 10 },
  dot: { width: 8, height: 8, borderRadius: 4 },
  wishlistBtn: {
    position: "absolute",
    top: 12,
    right: 12,
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: "center",
    justifyContent: "center",
  },
});
