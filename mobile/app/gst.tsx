import { RefreshControl, ScrollView, View } from "react-native";
import { AppText, Card, ErrorView, LoadingView, Screen } from "@/shared/components/ui";
import { useGstr3b } from "@/features/compliance/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { dateOnly, inr } from "@/shared/format";
import { ApiError } from "@/core/api/client";

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <View style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 5 }}>
      <AppText size={13} weight={strong ? "800" : "400"} style={{ flex: 1, paddingRight: 8 }}>{label}</AppText>
      <AppText size={13} weight={strong ? "800" : "600"}>{value}</AppText>
    </View>
  );
}

export default function Gst() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch, isRefetching } = useGstr3b();

  if (isLoading) return <Screen><LoadingView label="Loading GST summary…" /></Screen>;
  if (isError || !data) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  const g = data;
  return (
    <Screen padded={false}>
      <ScrollView
        contentContainerStyle={{ padding: 16, gap: 12 }}
        refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={t.accent} />}
      >
        <AppText weight="800" size={18}>GSTR-3B</AppText>
        <AppText muted size={12}>{dateOnly(g.from)} → {dateOnly(g.to)}</AppText>

        <Card style={{ padding: 14 }}>
          <AppText weight="700" size={14} style={{ marginBottom: 6 }}>Outward (sales)</AppText>
          <Row label="Taxable value" value={inr(g.outwardTaxable)} />
          <Row label="CGST" value={inr(g.outwardCgst)} />
          <Row label="SGST" value={inr(g.outwardSgst)} />
          <Row label="IGST" value={inr(g.outwardIgst)} />
        </Card>

        <Card style={{ padding: 14 }}>
          <AppText weight="700" size={14} style={{ marginBottom: 6 }}>Inward (ITC)</AppText>
          <Row label="Taxable value" value={inr(g.inwardTaxable)} />
          <Row label="ITC CGST" value={inr(g.itcCgst)} />
          <Row label="ITC SGST" value={inr(g.itcSgst)} />
          <Row label="ITC IGST" value={inr(g.itcIgst)} />
        </Card>

        <Card style={{ padding: 14 }}>
          <AppText weight="700" size={14} style={{ marginBottom: 6 }}>Net payable</AppText>
          <Row label="CGST" value={inr(g.netCgst)} />
          <Row label="SGST" value={inr(g.netSgst)} />
          <Row label="IGST" value={inr(g.netIgst)} />
          <View style={{ height: 1, backgroundColor: t.border, marginVertical: 6 }} />
          <Row label="Total GST payable" value={inr(g.netPayable)} strong />
        </Card>

        <AppText muted size={11} style={{ textAlign: "center" }}>Working figures — file the actual return on the GST portal.</AppText>
      </ScrollView>
    </Screen>
  );
}
