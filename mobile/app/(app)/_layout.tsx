import { useEffect, useRef } from "react";
import { Redirect, Tabs } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useAuth } from "@/core/auth/authStore";
import { useTheme } from "@/core/theme/ThemeProvider";
import { biometricAvailable, biometricUnlock } from "@/core/auth/biometric";

export default function AppLayout() {
  const t = useTheme();
  const status = useAuth((s) => s.status);
  const logout = useAuth((s) => s.logout);
  const gated = useRef(false);

  // One-time biometric gate on entering the authed area (if the device supports it).
  useEffect(() => {
    if (status !== "authed" || gated.current) return;
    gated.current = true;
    let active = true;
    (async () => {
      if (await biometricAvailable()) {
        const ok = await biometricUnlock("Unlock JL Admin");
        if (active && !ok) await logout();
      }
    })();
    return () => {
      active = false;
    };
  }, [status, logout]);

  if (status === "loading") return null;
  if (status === "guest") return <Redirect href="/login" />;

  return (
    <Tabs
      screenOptions={{
        headerStyle: { backgroundColor: t.surface },
        headerTitleStyle: { color: t.text },
        headerShadowVisible: false,
        tabBarActiveTintColor: t.accent,
        tabBarInactiveTintColor: t.textMuted,
        tabBarStyle: { backgroundColor: t.surface, borderTopColor: t.border },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{ title: "Dashboard", tabBarIcon: ({ color, size }) => <Ionicons name="grid-outline" color={color} size={size} /> }}
      />
      <Tabs.Screen
        name="orders"
        options={{ title: "Orders", tabBarIcon: ({ color, size }) => <Ionicons name="receipt-outline" color={color} size={size} /> }}
      />
      <Tabs.Screen
        name="products"
        options={{ title: "Products", tabBarIcon: ({ color, size }) => <Ionicons name="cube-outline" color={color} size={size} /> }}
      />
      <Tabs.Screen
        name="more"
        options={{ title: "More", tabBarIcon: ({ color, size }) => <Ionicons name="menu-outline" color={color} size={size} /> }}
      />
    </Tabs>
  );
}
