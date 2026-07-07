import { ScrollView, View } from "react-native";
import { AppText, Card, Screen } from "@/shared/components/ui";
import { useTheme } from "@/core/theme/ThemeProvider";

export default function DataExport() {
  const t = useTheme();
  const items = [
    ["📒", "Chart of accounts (CSV)"],
    ["✍️", "Journal entries (CSV)"],
    ["🧾", "Documents / invoices (CSV)"],
    ["📊", "Trial balance (CSV)"],
    ["🔗", "Tally-compatible export (XML)"],
    ["💾", "Full data backup"],
  ];
  return (
    <Screen padded={false}>
      <ScrollView contentContainerStyle={{ padding: 16, gap: 12 }}>
        <Card style={{ padding: 16 }}>
          <AppText weight="800" size={16} style={{ marginBottom: 6 }}>Import / Export</AppText>
          <AppText muted size={13}>
            Data import and file exports (CSV, Tally XML, backups) are handled on the web admin, where files
            can be downloaded and saved. Open <AppText weight="700" size={13}>Import / Export</AppText> under
            Control on the web admin at jlstores.in/admin.
          </AppText>
        </Card>

        <Card style={{ paddingVertical: 4 }}>
          {items.map(([icon, label], i) => (
            <View key={label} style={{ flexDirection: "row", alignItems: "center", gap: 12, paddingVertical: 12, borderBottomWidth: i < items.length - 1 ? 1 : 0, borderBottomColor: t.border }}>
              <AppText size={18}>{icon}</AppText>
              <AppText size={14} style={{ flex: 1 }}>{label}</AppText>
              <AppText muted size={11}>Web</AppText>
            </View>
          ))}
        </Card>
      </ScrollView>
    </Screen>
  );
}
