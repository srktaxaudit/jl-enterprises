import "react-native-gesture-handler";
import { useEffect } from "react";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "@/core/api/queryClient";
import { ThemeProvider } from "@/core/theme/ThemeProvider";
import { useAuth } from "@/core/auth/authStore";

export default function RootLayout() {
  const restore = useAuth((s) => s.restore);
  useEffect(() => {
    void restore();
  }, [restore]);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider>
            <StatusBar style="auto" />
            <Stack screenOptions={{ headerShown: false }}>
              <Stack.Screen name="product/[slug]" options={{ headerShown: true, title: "Product" }} />
              <Stack.Screen name="order/[id]" options={{ headerShown: true, title: "Order" }} />
              <Stack.Screen name="checkout" options={{ headerShown: true, title: "Checkout" }} />
              <Stack.Screen name="addresses" options={{ headerShown: true, title: "My Addresses" }} />
              <Stack.Screen name="wishlist" options={{ headerShown: true, title: "My Wishlist" }} />
              <Stack.Screen name="track-order" options={{ headerShown: true, title: "Track Order" }} />
            </Stack>
          </ThemeProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
