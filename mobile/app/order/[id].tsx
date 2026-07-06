import { ScrollView, View } from "react-native";
import { Stack, useLocalSearchParams } from "expo-router";
import { useQuery } from "@tanstack/react-query";
import { apiGet, ApiError } from "@/core/api/client";
import { AppText, Button, Card, ErrorView, LoadingView, Screen, StatusBadge } from "@/shared/components/ui";
import { orderTone, useUpdateOrderStatus } from "@/features/orders/hooks";
import { dateTime, inr } from "@/shared/format";

interface OrderItem {
  id?: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}
interface OrderDetail {
  id: string;
  orderNumber: string;
  status: string;
  subtotal?: number;
  discountTotal?: number;
  shippingTotal?: number;
  grandTotal: number;
  placedAt?: string;
  items?: OrderItem[];
}

const TRANSITIONS = ["CONFIRMED", "PACKED", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"];

export default function OrderDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const q = useQuery({
    queryKey: ["orders", "detail", id],
    queryFn: () => apiGet<OrderDetail>(`/api/v1/admin/orders/${id}`),
    enabled: !!id,
  });
  const mut = useUpdateOrderStatus();

  if (q.isLoading) return <Screen><LoadingView /></Screen>;
  if (q.isError || !q.data) {
    return <Screen><ErrorView message={q.error instanceof ApiError ? q.error.message : "Network error"} onRetry={q.refetch} /></Screen>;
  }

  const o = q.data;
  return (
    <Screen padded={false}>
      <Stack.Screen options={{ title: `#${o.orderNumber}` }} />
      <ScrollView contentContainerStyle={{ padding: 16, gap: 12 }}>
        <Card>
          <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
            <StatusBadge label={o.status} tone={orderTone(o.status)} />
            <AppText weight="800" size={18}>{inr(o.grandTotal)}</AppText>
          </View>
          <AppText muted size={12} style={{ marginTop: 8 }}>Placed {dateTime(o.placedAt)}</AppText>
        </Card>

        {o.items?.length ? (
          <Card>
            <AppText weight="700" size={14} style={{ marginBottom: 8 }}>Items</AppText>
            {o.items.map((it, i) => (
              <View key={it.id ?? i} style={{ flexDirection: "row", justifyContent: "space-between", paddingVertical: 6 }}>
                <AppText size={13} style={{ flex: 1, paddingRight: 8 }} numberOfLines={1}>{it.productName} × {it.quantity}</AppText>
                <AppText size={13} weight="600">{inr(it.lineTotal)}</AppText>
              </View>
            ))}
          </Card>
        ) : null}

        <Card>
          <AppText weight="700" size={14} style={{ marginBottom: 10 }}>Update status</AppText>
          <View style={{ gap: 8 }}>
            {TRANSITIONS.filter((s) => s !== o.status).map((s) => (
              <Button
                key={s}
                title={s.replace(/_/g, " ")}
                variant="outline"
                loading={mut.isPending && mut.variables?.status === s}
                onPress={() => mut.mutate({ id: o.id, status: s }, { onSuccess: () => q.refetch() })}
              />
            ))}
          </View>
          <AppText muted size={11} style={{ marginTop: 10 }}>
            Verify the status endpoint + allowed transitions against AdminOrderController before shipping.
          </AppText>
        </Card>
      </ScrollView>
    </Screen>
  );
}
