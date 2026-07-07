import { useState } from "react";
import { ScrollView, Switch, View } from "react-native";
import { AppText, Button, Card, Screen, TextField } from "@/shared/components/ui";
import { useBroadcast } from "@/features/whatsapp/hooks";
import { useTheme } from "@/core/theme/ThemeProvider";
import { ApiError } from "@/core/api/client";

export default function WhatsApp() {
  const t = useTheme();
  const [message, setMessage] = useState("");
  const [onlyVerified, setOnlyVerified] = useState(true);
  const broadcast = useBroadcast();

  const send = () => {
    const m = message.trim();
    if (!m) return;
    broadcast.mutate({ message: m, onlyVerified });
  };

  const r = broadcast.data;

  return (
    <Screen padded={false}>
      <ScrollView contentContainerStyle={{ padding: 16, gap: 14 }}>
        <AppText muted size={13}>
          Send a promotional WhatsApp message to customers who have a phone number on file.
        </AppText>

        <TextField
          label="Message"
          placeholder="Type your offer…"
          value={message}
          onChangeText={setMessage}
          multiline
          style={{ minHeight: 120, textAlignVertical: "top" }}
        />

        <Card style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", padding: 14 }}>
          <View style={{ flex: 1, paddingRight: 12 }}>
            <AppText weight="700" size={14}>Verified numbers only</AppText>
            <AppText muted size={12} style={{ marginTop: 2 }}>Send to customers with a verified phone.</AppText>
          </View>
          <Switch
            value={onlyVerified}
            onValueChange={setOnlyVerified}
            trackColor={{ true: t.accent, false: t.border }}
            thumbColor="#fff"
          />
        </Card>

        <Button title="Send broadcast" onPress={send} loading={broadcast.isPending} disabled={!message.trim()} />

        {broadcast.isError ? (
          <AppText color={t.danger} size={13}>
            {broadcast.error instanceof ApiError ? broadcast.error.message : "Could not send"}
          </AppText>
        ) : null}

        {r ? (
          <Card style={{ padding: 14 }}>
            <AppText weight="800" size={15} style={{ marginBottom: 6 }}>
              {r.demoMode ? "Demo mode" : "Broadcast complete"}
            </AppText>
            <AppText muted size={13}>
              {r.recipients} recipient(s) · {r.sent} sent · {r.failed} failed
            </AppText>
            {r.demoMode ? (
              <AppText muted size={12} style={{ marginTop: 6 }}>
                No WhatsApp credentials configured — messages were logged, not actually sent.
              </AppText>
            ) : null}
          </Card>
        ) : null}
      </ScrollView>
    </Screen>
  );
}
