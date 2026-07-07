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
              <Stack.Screen name="order/[id]" options={{ headerShown: true, title: "Order" }} />
              <Stack.Screen name="inventory" options={{ headerShown: true, title: "Inventory" }} />
              <Stack.Screen name="offers" options={{ headerShown: true, title: "Offers & Deals" }} />
              <Stack.Screen name="customers" options={{ headerShown: true, title: "Customers" }} />
              <Stack.Screen name="reviews" options={{ headerShown: true, title: "Reviews" }} />
              <Stack.Screen name="service" options={{ headerShown: true, title: "Service Bookings" }} />
              <Stack.Screen name="exchange" options={{ headerShown: true, title: "Exchange Requests" }} />
              <Stack.Screen name="staff" options={{ headerShown: true, title: "Staff" }} />
              <Stack.Screen name="team" options={{ headerShown: true, title: "Team & Roles" }} />
              <Stack.Screen name="logs" options={{ headerShown: true, title: "Activity Logs" }} />
              <Stack.Screen name="settings" options={{ headerShown: true, title: "Settings" }} />
              <Stack.Screen name="whatsapp" options={{ headerShown: true, title: "WhatsApp Offers" }} />
              <Stack.Screen name="branding" options={{ headerShown: true, title: "Logo & Branding" }} />
            </Stack>
          </ThemeProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
