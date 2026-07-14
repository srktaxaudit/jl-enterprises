import React from "react";
import { FlatList, Pressable, StyleSheet, View } from "react-native";
import { Image } from "expo-image";
import { useRouter } from "expo-router";
import { AppText, Button, Card, EmptyState, ErrorView, LoadingView, QtyStepper, Screen } from "@/shared/components/ui";
import { SignInPrompt } from "@/shared/components/SignInPrompt";
import { useTheme } from "@/core/theme/ThemeProvider";
import { useAuth } from "@/core/auth/authStore";
import { useCart, useRemoveCartItem, useUpdateCartItem } from "@/features/cart/hooks";
import { inr } from "@/shared/format";
import type { CartItem } from "@/core/types";

export default function CartTab() {
  const t = useTheme();
  const router = useRouter();
  const status = useAuth((s) => s.status);
  const cart = useCart();
  const updateItem = useUpdateCartItem();
  const removeItem = useRemoveCartItem();

  if (status === "loading") return <Screen><LoadingView /></Screen>;
  if (status === "guest") {
    return (
      <Screen>
        <SignInPrompt message="Sign in to see your cart and check out." />
      </Screen>
    );
  }
  if (cart.isLoading) return <Screen><LoadingView label="Loading cart…" /></Screen>;
  if (cart.isError) {
    return <Screen><ErrorView message={(cart.error as Error).message} onRetry={() => cart.refetch()} /></Screen>;
  }

  const items = cart.data?.items ?? [];
  if (items.length === 0) {
    return (
      <Screen>
        <EmptyState emoji="🛒" title="Your cart is empty" hint="Browse the shop and add something you like." />
        <Button title="Start shopping" variant="accent" onPress={() => router.push("/(tabs)/shop")} />
      </Screen>
    );
  }

  const renderItem = ({ item }: { item: CartItem }) => (
    <Card style={{ marginBottom: 10, flexDirection: "row", gap: 12, padding: 12 }}>
      <Pressable onPress={() => router.push(`/product/${item.slug}`)}>
        <View style={[styles.thumb, { backgroundColor: t.surfaceAlt }]}>
          {item.primaryImageUrl ? (
            <Image source={{ uri: item.primaryImageUrl }} style={{ width: "100%", height: "100%" }} contentFit="contain" />
          ) : (
            <AppText size={22}>🛋️</AppText>
          )}
        </View>
      </Pressable>
      <View style={{ flex: 1, gap: 6 }}>
        <AppText weight="600" size={13} numberOfLines={2}>{item.productName}</AppText>
        <AppText weight="800" size={15}>{inr(item.lineTotal)}</AppText>
        <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
          <QtyStepper
            value={item.quantity}
            onChange={(q) => updateItem.mutate({ itemId: item.id, quantity: q })}
          />
          <Pressable onPress={() => removeItem.mutate(item.id)} hitSlop={8}>
            <AppText size={12} weight="700" color={t.danger}>Remove</AppText>
          </Pressable>
        </View>
      </View>
    </Card>
  );

  return (
    <Screen>
      <FlatList
        data={items}
        keyExtractor={(i) => i.id}
        renderItem={renderItem}
        refreshing={cart.isRefetching}
        onRefresh={() => void cart.refetch()}
        contentContainerStyle={{ paddingBottom: 12 }}
      />
      <Card style={{ gap: 10 }}>
        <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
          <AppText muted>Subtotal ({cart.data?.itemCount} items)</AppText>
          <AppText weight="800" size={17}>{inr(cart.data?.subtotal)}</AppText>
        </View>
        <AppText muted size={12}>Delivery and any coupon discount are calculated at checkout.</AppText>
        <Button title="Proceed to checkout" variant="accent" onPress={() => router.push("/checkout")} />
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  thumb: { width: 76, height: 76, borderRadius: 12, overflow: "hidden", alignItems: "center", justifyContent: "center" },
});
