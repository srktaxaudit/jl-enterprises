import React from "react";
import { Alert, ScrollView, View } from "react-native";
import { Stack, useLocalSearchParams } from "expo-router";
import { AppText, Button, Card, ErrorView, LoadingView, Screen, StatusBadge } from "@/shared/components/ui";
import { useTheme } from "@/core/theme/ThemeProvider";
import { useCancelOrder, useOrder, useRequestReturn } from "@/features/orders/hooks";
import { statusTone } from "@/shared/orderStatus";
import { ApiError } from "@/core/api/client";
import { dateTime, inr, statusLabel } from "@/shared/format";
import type { OrderStatus } from "@/core/types";

const CANCELLABLE: OrderStatus[] = ["PENDING", "CONFIRMED", "PROCESSING", "PACKED"];

export default function OrderDetail() {
  const t = useTheme();
  const { id } = useLocalSearchParams<{ id: string }>();
  const order = useOrder(id);
  const cancel = useCancelOrder(id);
  const requestReturn = useRequestReturn(id);

  if (order.isLoading) return <Screen><LoadingView label="Loading order…" /></Screen>;
  if (order.isError || !order.data) {
    return <Screen><ErrorView message={(order.error as Error)?.message ?? "Not found"} onRetry={() => order.refetch()} /></Screen>;
  }

  const o = order.data;
  const addr = o.shippingAddress;
  const canCancel = CANCELLABLE.includes(o.status);
  const canReturn = o.status === "DELIVERED";

  const confirm = (title: string, message: string, run: () => void) =>
    Alert.alert(title, message, [
      { text: "No", style: "cancel" },
      { text: "Yes", style: "destructive", onPress: run },
    ]);

  const onError = (e: unknown) =>
    Alert.alert("Something went wrong", e instanceof ApiError ? e.message : "Please try again.");

  return (
    <Screen>
      <Stack.Screen options={{ title: `Order #${o.orderNumber}` }} />
      <ScrollView contentContainerStyle={{ gap: 12, paddingBottom: 24 }}>
        <Card style={{ gap: 8 }}>
          <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
            <AppText weight="800" size={16}>#{o.orderNumber}</AppText>
            <StatusBadge label={statusLabel(o.status)} tone={statusTone(o.status)} />
          </View>
          <AppText muted size={12}>Placed {dateTime(o.placedAt)}</AppText>
          {o.payment?.method ? (
            <AppText muted size={12}>Payment: {o.payment.method === "COD" ? "Cash on delivery" : o.payment.method}</AppText>
          ) : null}
          {o.cancellationReason ? <AppText size={12} color={t.danger}>Cancelled: {o.cancellationReason}</AppText> : null}
          {o.returnReason ? <AppText size={12} color={t.warn}>Return: {o.returnReason}</AppText> : null}
        </Card>

        <Card style={{ gap: 8 }}>
          <AppText weight="700" size={14}>Items</AppText>
          {o.items.map((i) => (
            <View key={i.id} style={{ flexDirection: "row", justifyContent: "space-between" }}>
              <AppText size={13} numberOfLines={1} style={{ flex: 1, marginRight: 8 }}>
                {i.productName} × {i.quantity}
              </AppText>
              <AppText size={13} weight="600">{inr(i.lineTotal)}</AppText>
            </View>
          ))}
          <View style={{ height: 1, backgroundColor: t.border }} />
          <SummaryRow label="Subtotal" value={inr(o.subtotal)} />
          {Number(o.discountTotal) > 0 ? <SummaryRow label={`Discount${o.couponCode ? ` (${o.couponCode})` : ""}`} value={`−${inr(o.discountTotal)}`} /> : null}
          {Number(o.taxTotal) > 0 ? <SummaryRow label="Tax" value={inr(o.taxTotal)} /> : null}
          <SummaryRow label="Delivery" value={Number(o.shippingTotal) > 0 ? inr(o.shippingTotal) : "Free"} />
          <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
            <AppText weight="800">Total</AppText>
            <AppText weight="800" size={16}>{inr(o.grandTotal)}</AppText>
          </View>
        </Card>

        {addr ? (
          <Card style={{ gap: 4 }}>
            <AppText weight="700" size={14}>Delivery address</AppText>
            {addr.fullName ? <AppText size={13}>{addr.fullName}</AppText> : null}
            <AppText muted size={13}>
              {[addr.line1, addr.line2, addr.city, addr.state, addr.postalCode].filter(Boolean).join(", ")}
            </AppText>
            {addr.phone ? <AppText muted size={13}>📞 {addr.phone}</AppText> : null}
          </Card>
        ) : null}

        {canCancel ? (
          <Button
            title="Cancel order"
            variant="danger"
            loading={cancel.isPending}
            onPress={() =>
              confirm("Cancel order?", "This can't be undone.", () =>
                cancel.mutate(undefined, { onError }),
              )
            }
          />
        ) : null}
        {canReturn ? (
          <Button
            title="Request a return"
            variant="outline"
            loading={requestReturn.isPending}
            onPress={() =>
              confirm("Request a return?", "Our team will contact you to arrange pickup.", () =>
                requestReturn.mutate(undefined, { onError }),
              )
            }
          />
        ) : null}
      </ScrollView>
    </Screen>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
      <AppText muted size={13}>{label}</AppText>
      <AppText size={13} weight="600">{value}</AppText>
    </View>
  );
}
