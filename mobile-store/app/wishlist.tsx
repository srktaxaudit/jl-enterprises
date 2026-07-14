import React from "react";
import { FlatList, View } from "react-native";
import { EmptyState, ErrorView, LoadingView, Screen } from "@/shared/components/ui";
import { SignInPrompt } from "@/shared/components/SignInPrompt";
import { ProductCard } from "@/shared/components/ProductCard";
import { useAuth } from "@/core/auth/authStore";
import { useWishlist } from "@/features/wishlist/hooks";

export default function WishlistPage() {
  const status = useAuth((s) => s.status);
  const wishlist = useWishlist();

  if (status === "guest") return <Screen><SignInPrompt message="Sign in to see your wishlist." /></Screen>;
  if (wishlist.isLoading) return <Screen><LoadingView label="Loading wishlist…" /></Screen>;
  if (wishlist.isError) {
    return <Screen><ErrorView message={(wishlist.error as Error).message} onRetry={() => wishlist.refetch()} /></Screen>;
  }

  const items = wishlist.data?.items ?? [];
  if (items.length === 0) {
    return <Screen><EmptyState emoji="💝" title="Your wishlist is empty" hint="Tap the heart on a product to save it here." /></Screen>;
  }

  return (
    <Screen padded={false}>
      <FlatList
        data={items}
        numColumns={2}
        keyExtractor={(i) => i.id}
        contentContainerStyle={{ padding: 10 }}
        refreshing={wishlist.isRefetching}
        onRefresh={() => void wishlist.refetch()}
        renderItem={({ item }) => (
          <View style={{ width: "50%" }}>
            <ProductCard product={item.product} />
          </View>
        )}
      />
    </Screen>
  );
}
