import React, { useState } from "react";
import { KeyboardAvoidingView, Platform, ScrollView } from "react-native";
import { AppText, Button, Card, Screen, StatusBadge, TextField } from "@/shared/components/ui";
import { useTheme } from "@/core/theme/ThemeProvider";
import { useTrackOrder } from "@/features/orders/hooks";
import { ApiError } from "@/core/api/client";
import { statusTone } from "@/shared/orderStatus";
import { dateTime, statusLabel } from "@/shared/format";

/** Public tracking — order number + phone, no sign-in needed. */
export default function TrackOrder() {
  const t = useTheme();
  const track = useTrackOrder();
  const [number, setNumber] = useState("");
  const [phone, setPhone] = useState("");
  const [error, setError] = useState<string | null>(null);

  const onTrack = () => {
    setError(null);
    if (!number.trim() || !phone.trim()) {
      setError("Enter both the order number and the phone used on the order.");
      return;
    }
    track.mutate(
      { number: number.trim(), phone: phone.trim() },
      { onError: (e) => setError(e instanceof ApiError ? e.message : "Couldn't find that order.") },
    );
  };

  return (
    <Screen>
      <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1 }}>
        <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ gap: 4, paddingTop: 8 }}>
          <AppText muted size={13} style={{ marginBottom: 12 }}>
            Enter your order number and the phone number used when placing the order.
          </AppText>
          <TextField
            label="Order number"
            placeholder="e.g. JL-2026-000123"
            value={number}
            onChangeText={setNumber}
            autoCapitalize="characters"
          />
          <TextField
            label="Phone number"
            placeholder="98xxxxxxxx"
            value={phone}
            onChangeText={setPhone}
            keyboardType="phone-pad"
          />
          {error ? <AppText color={t.danger} size={13} style={{ marginBottom: 10 }}>{error}</AppText> : null}
          <Button title="Track order" onPress={onTrack} loading={track.isPending} />

          {track.data ? (
            <Card style={{ marginTop: 20, gap: 8 }}>
              <AppText weight="800" size={16}>#{track.data.orderNumber}</AppText>
              <StatusBadge label={statusLabel(track.data.status)} tone={statusTone(track.data.status)} />
              <AppText muted size={13}>Placed {dateTime(track.data.placedAt)}</AppText>
            </Card>
          ) : null}
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
