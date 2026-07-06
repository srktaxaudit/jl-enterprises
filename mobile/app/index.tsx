import { Redirect } from "expo-router";
import { View, ActivityIndicator } from "react-native";
import { useAuth } from "@/core/auth/authStore";

/** Entry gate: waits for session restore, then routes to app or login. */
export default function Index() {
  const status = useAuth((s) => s.status);
  if (status === "loading") {
    return (
      <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
        <ActivityIndicator />
      </View>
    );
  }
  return <Redirect href={status === "authed" ? "/(app)/" : "/login"} />;
}
