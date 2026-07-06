import { Alert, Pressable, ScrollView, View } from "react-native";
import { useRouter } from "expo-router";
import { AppText, Button, Card, Screen } from "@/shared/components/ui";
import { GROUP_ORDER, MODULES, type AdminModule } from "@/core/navigation/modules";
import { useAuth } from "@/core/auth/authStore";
import { canSee, displayName, initials } from "@/core/auth/rbac";
import { useTheme } from "@/core/theme/ThemeProvider";

function Row({ m, onPress }: { m: AdminModule; onPress: () => void }) {
  const t = useTheme();
  return (
    <Pressable onPress={onPress} style={({ pressed }) => ({ opacity: pressed ? 0.6 : 1 })}>
      <View style={{ flexDirection: "row", alignItems: "center", paddingVertical: 12, gap: 12 }}>
        <AppText size={18}>{m.icon}</AppText>
        <AppText size={15} style={{ flex: 1 }}>{m.label}</AppText>
        {!m.built ? <AppText muted size={11}>Soon</AppText> : <AppText muted size={16}>›</AppText>}
      </View>
      <View style={{ height: 1, backgroundColor: t.border }} />
    </Pressable>
  );
}

export default function More() {
  const t = useTheme();
  const router = useRouter();
  const user = useAuth((s) => s.user);
  const logout = useAuth((s) => s.logout);

  const open = (m: AdminModule) => {
    if (m.built) router.push(m.route as never);
    else Alert.alert(m.label, "This module is scaffolded and follows the Orders/Products pattern — wire its data hook + screen to finish it.");
  };

  return (
    <Screen padded={false}>
      <ScrollView contentContainerStyle={{ padding: 16 }}>
        <Card style={{ flexDirection: "row", alignItems: "center", gap: 12, marginBottom: 16 }}>
          <View style={{ width: 44, height: 44, borderRadius: 22, backgroundColor: t.primary, alignItems: "center", justifyContent: "center" }}>
            <AppText color="#fff" weight="800">{initials(user)}</AppText>
          </View>
          <View style={{ flex: 1 }}>
            <AppText weight="700" size={15}>{displayName(user)}</AppText>
            <AppText muted size={12}>{(user?.roles ?? []).map((r) => r.replace("ROLE_", "")).join(", ")}</AppText>
          </View>
        </Card>

        {GROUP_ORDER.map((group) => {
          const items = MODULES.filter((m) => m.group === group && m.key !== "dashboard" && m.key !== "orders" && m.key !== "products" && canSee(user, m.rule));
          if (!items.length) return null;
          return (
            <View key={group} style={{ marginBottom: 18 }}>
              <AppText muted size={11} weight="700" style={{ letterSpacing: 1, marginBottom: 4 }}>{group.toUpperCase()}</AppText>
              <Card style={{ paddingVertical: 0 }}>
                {items.map((m) => <Row key={m.key} m={m} onPress={() => open(m)} />)}
              </Card>
            </View>
          );
        })}

        <Button title="Log out" variant="danger" onPress={() => void logout()} />
        <AppText muted size={11} style={{ textAlign: "center", marginTop: 14 }}>JL Admin · dark mode follows your device</AppText>
      </ScrollView>
    </Screen>
  );
}
