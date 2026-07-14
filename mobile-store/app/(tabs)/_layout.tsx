import { Tabs } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useTheme } from "@/core/theme/ThemeProvider";
import { useCart } from "@/features/cart/hooks";

// Browsing is open to guests — auth is only required inside the cart,
// orders and account flows, which each show a sign-in prompt themselves.
export default function TabsLayout() {
  const t = useTheme();
  const { data: cart } = useCart();
  const cartCount = cart?.itemCount ?? 0;

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
        options={{
          title: "Home",
          headerTitle: "JL Enterprises",
          tabBarIcon: ({ color, size }) => <Ionicons name="home-outline" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="shop"
        options={{
          title: "Shop",
          tabBarIcon: ({ color, size }) => <Ionicons name="search-outline" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="cart"
        options={{
          title: "Cart",
          tabBarBadge: cartCount > 0 ? cartCount : undefined,
          tabBarBadgeStyle: { backgroundColor: t.accent, color: t.onPrimary },
          tabBarIcon: ({ color, size }) => <Ionicons name="cart-outline" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="orders"
        options={{
          title: "Orders",
          headerTitle: "My Orders",
          tabBarIcon: ({ color, size }) => <Ionicons name="receipt-outline" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="account"
        options={{
          title: "Account",
          tabBarIcon: ({ color, size }) => <Ionicons name="person-outline" color={color} size={size} />,
        }}
      />
    </Tabs>
  );
}
