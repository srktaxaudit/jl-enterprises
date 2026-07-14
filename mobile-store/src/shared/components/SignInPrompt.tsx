import React from "react";
import { Text, View } from "react-native";
import { useRouter } from "expo-router";
import { AppText, Button } from "./ui";

/** Shown in place of auth-only content (cart, orders, wishlist…) for guests. */
export function SignInPrompt({ message }: { message: string }) {
  const router = useRouter();
  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", padding: 32 }}>
      <Text style={{ fontSize: 40, marginBottom: 8 }}>🔐</Text>
      <AppText weight="700" size={16}>Sign in required</AppText>
      <AppText muted size={13} style={{ marginTop: 4, textAlign: "center" }}>{message}</AppText>
      <View style={{ marginTop: 16, alignSelf: "stretch", gap: 10 }}>
        <Button title="Sign in" onPress={() => router.push("/login")} />
        <Button title="Create an account" variant="outline" onPress={() => router.push("/signup")} />
      </View>
    </View>
  );
}
