import React from "react";
import { Pressable, ScrollView, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import { AppText, Button, Card, Screen } from "@/shared/components/ui";
import { SignInPrompt } from "@/shared/components/SignInPrompt";
import { useTheme } from "@/core/theme/ThemeProvider";
import { useAuth } from "@/core/auth/authStore";

export default function AccountTab() {
  const t = useTheme();
  const router = useRouter();
  const { user, status, logout } = useAuth();

  if (status === "guest") {
    return (
      <Screen>
        <SignInPrompt message="Sign in to manage your profile, addresses and wishlist." />
        <Row icon="location-outline" label="Track an order (no sign-in needed)" onPress={() => router.push("/track-order")} />
      </Screen>
    );
  }

  const name = [user?.firstName, user?.lastName].filter(Boolean).join(" ") || user?.email || "Customer";

  return (
    <Screen>
      <ScrollView contentContainerStyle={{ paddingBottom: 24 }}>
        <Card style={{ alignItems: "center", gap: 6, marginBottom: 16 }}>
          <View style={{ width: 64, height: 64, borderRadius: 32, backgroundColor: t.primary, alignItems: "center", justifyContent: "center" }}>
            <AppText color={t.onPrimary} weight="800" size={24}>{name.charAt(0).toUpperCase()}</AppText>
          </View>
          <AppText weight="800" size={17}>{name}</AppText>
          <AppText muted size={13}>{user?.email}</AppText>
          {user?.phone ? <AppText muted size={13}>{user.phone}</AppText> : null}
        </Card>

        <Card style={{ padding: 4, marginBottom: 16 }}>
          <Row icon="heart-outline" label="My wishlist" onPress={() => router.push("/wishlist")} />
          <Row icon="home-outline" label="My addresses" onPress={() => router.push("/addresses")} />
          <Row icon="location-outline" label="Track an order" onPress={() => router.push("/track-order")} />
        </Card>

        <Button title="Sign out" variant="outline" onPress={() => void logout()} />
      </ScrollView>
    </Screen>
  );
}

function Row({ icon, label, onPress }: { icon: keyof typeof Ionicons.glyphMap; label: string; onPress: () => void }) {
  const t = useTheme();
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => ({
        flexDirection: "row",
        alignItems: "center",
        gap: 12,
        padding: 14,
        opacity: pressed ? 0.7 : 1,
      })}
    >
      <Ionicons name={icon} size={20} color={t.accent} />
      <AppText weight="600" size={14} style={{ flex: 1 }}>{label}</AppText>
      <Ionicons name="chevron-forward" size={16} color={t.textMuted} />
    </Pressable>
  );
}
