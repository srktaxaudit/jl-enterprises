import { ScrollView, View } from "react-native";
import { Image } from "expo-image";
import { AppText, Card, ErrorView, LoadingView, Screen } from "@/shared/components/ui";
import { useBranding } from "@/features/branding/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { ApiError } from "@/core/api/client";

export default function Branding() {
  const t = useTheme();
  const { data, isLoading, isError, error, refetch } = useBranding();

  if (isLoading) return <Screen><LoadingView label="Loading branding…" /></Screen>;
  if (isError) return <Screen><ErrorView message={error instanceof ApiError ? error.message : "Network error"} onRetry={refetch} /></Screen>;

  return (
    <Screen>
      <ScrollView contentContainerStyle={{ gap: 14 }}>
        <Card style={{ alignItems: "center", padding: 24 }}>
          {data?.logoUrl ? (
            <Image source={{ uri: data.logoUrl }} style={{ width: 180, height: 90 }} contentFit="contain" transition={150} />
          ) : (
            <View style={{ width: 180, height: 90, borderRadius: 12, backgroundColor: t.surfaceAlt, alignItems: "center", justifyContent: "center" }}>
              <AppText size={30}>🖼️</AppText>
            </View>
          )}
          <AppText weight="800" size={18} style={{ marginTop: 14 }}>{data?.siteName || "JL Enterprises"}</AppText>
          <AppText muted size={12} style={{ marginTop: 4 }}>{data?.logoUrl ? "Current logo" : "No logo uploaded yet"}</AppText>
        </Card>

        <Card style={{ padding: 14 }}>
          <AppText size={13} muted>
            Upload or replace the logo from the web admin (Logo &amp; Branding). It updates everywhere — storefront, admin and this app.
          </AppText>
        </Card>
      </ScrollView>
    </Screen>
  );
}
