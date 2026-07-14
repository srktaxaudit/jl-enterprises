import React, { useEffect, useState } from "react";
import { Alert, Pressable, ScrollView, View } from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import {
  AppText,
  Button,
  Card,
  EmptyState,
  ErrorView,
  LoadingView,
  Screen,
  TextField,
} from "@/shared/components/ui";
import { SignInPrompt } from "@/shared/components/SignInPrompt";
import { useTheme } from "@/core/theme/ThemeProvider";
import { useAuth } from "@/core/auth/authStore";
import { useCart, useValidateCoupon } from "@/features/cart/hooks";
import { useAddresses } from "@/features/addresses/hooks";
import { usePlaceOrder } from "@/features/orders/hooks";
import { ApiError } from "@/core/api/client";
import { inr } from "@/shared/format";
import type { PaymentMethod } from "@/core/types";

// COD is live on the backend; Razorpay is wired server-side but the mobile
// payment sheet isn't integrated yet, so COD is the only method offered here.
const PAYMENT_METHODS: { value: PaymentMethod; label: string; hint: string }[] = [
  { value: "COD", label: "Cash on delivery", hint: "Pay when your order arrives" },
];

export default function Checkout() {
  const t = useTheme();
  const router = useRouter();
  const status = useAuth((s) => s.status);
  const cart = useCart();
  const addresses = useAddresses();
  const placeOrder = usePlaceOrder();
  const validateCoupon = useValidateCoupon();

  const [addressId, setAddressId] = useState<string | null>(null);
  const [payment, setPayment] = useState<PaymentMethod>("COD");
  const [couponCode, setCouponCode] = useState("");
  const [appliedCoupon, setAppliedCoupon] = useState<string | null>(null);
  const [couponMessage, setCouponMessage] = useState<string | null>(null);
  const [notes, setNotes] = useState("");

  // Preselect the default (or only) address once the list arrives.
  useEffect(() => {
    if (!addressId && addresses.data?.length) {
      const def = addresses.data.find((a) => a.defaultAddress) ?? addresses.data[0];
      setAddressId(def.id);
    }
  }, [addresses.data, addressId]);

  if (status === "guest") return <Screen><SignInPrompt message="Sign in to check out." /></Screen>;
  if (cart.isLoading || addresses.isLoading) return <Screen><LoadingView label="Loading checkout…" /></Screen>;
  if (cart.isError) return <Screen><ErrorView message={(cart.error as Error).message} onRetry={() => cart.refetch()} /></Screen>;

  const items = cart.data?.items ?? [];
  if (items.length === 0 && !placeOrder.isSuccess) {
    return <Screen><EmptyState emoji="🛒" title="Your cart is empty" hint="Add something before checking out." /></Screen>;
  }

  const onApplyCoupon = () => {
    const code = couponCode.trim().toUpperCase();
    if (!code) return;
    validateCoupon.mutate(code, {
      onSuccess: (res) => {
        if (res.valid) {
          setAppliedCoupon(code);
          setCouponMessage(res.discountAmount ? `Coupon applied — you save ${inr(res.discountAmount)}.` : "Coupon applied.");
        } else {
          setAppliedCoupon(null);
          setCouponMessage(res.message ?? "This coupon can't be applied.");
        }
      },
      onError: (e) => {
        setAppliedCoupon(null);
        setCouponMessage(e instanceof ApiError ? e.message : "Couldn't validate the coupon.");
      },
    });
  };

  const onPlaceOrder = () => {
    if (!addressId) {
      Alert.alert("Delivery address", "Please add and select a delivery address first.");
      return;
    }
    placeOrder.mutate(
      {
        shippingAddressId: addressId,
        couponCode: appliedCoupon ?? undefined,
        paymentMethod: payment,
        notes: notes.trim() || undefined,
      },
      {
        onSuccess: (order) => {
          router.replace(`/order/${order.id}`);
          Alert.alert("Order placed 🎉", `Your order #${order.orderNumber} has been placed.`);
        },
        onError: (e) => Alert.alert("Couldn't place order", e instanceof ApiError ? e.message : "Please try again."),
      },
    );
  };

  return (
    <Screen>
      <ScrollView contentContainerStyle={{ paddingBottom: 24, gap: 14 }} keyboardShouldPersistTaps="handled">
        {/* Address */}
        <View style={{ gap: 8 }}>
          <AppText weight="800" size={16}>Delivery address</AppText>
          {(addresses.data ?? []).map((a) => (
            <Pressable key={a.id} onPress={() => setAddressId(a.id)}>
              <Card
                style={{
                  padding: 12,
                  gap: 2,
                  borderColor: addressId === a.id ? t.accent : t.border,
                  borderWidth: addressId === a.id ? 2 : 1,
                }}
              >
                <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
                  <Ionicons
                    name={addressId === a.id ? "radio-button-on" : "radio-button-off"}
                    size={18}
                    color={addressId === a.id ? t.accent : t.textMuted}
                  />
                  <AppText weight="700" size={13}>{a.fullName ?? "Delivery address"}{a.defaultAddress ? " · Default" : ""}</AppText>
                </View>
                <AppText muted size={12}>
                  {[a.line1, a.line2, a.city, a.state, a.postalCode].filter(Boolean).join(", ")}
                </AppText>
                {a.phone ? <AppText muted size={12}>📞 {a.phone}</AppText> : null}
              </Card>
            </Pressable>
          ))}
          <Button title="+ Add a new address" variant="outline" onPress={() => router.push("/addresses")} />
        </View>

        {/* Coupon */}
        <View style={{ gap: 4 }}>
          <AppText weight="800" size={16}>Coupon</AppText>
          <View style={{ flexDirection: "row", gap: 8, alignItems: "flex-start" }}>
            <View style={{ flex: 1 }}>
              <TextField
                placeholder="Coupon code"
                value={couponCode}
                onChangeText={(v) => {
                  setCouponCode(v);
                  setAppliedCoupon(null);
                  setCouponMessage(null);
                }}
                autoCapitalize="characters"
              />
            </View>
            <Button title="Apply" variant="outline" loading={validateCoupon.isPending} onPress={onApplyCoupon} />
          </View>
          {couponMessage ? (
            <AppText size={12} color={appliedCoupon ? t.success : t.danger}>{couponMessage}</AppText>
          ) : null}
        </View>

        {/* Payment */}
        <View style={{ gap: 8 }}>
          <AppText weight="800" size={16}>Payment</AppText>
          {PAYMENT_METHODS.map((m) => (
            <Pressable key={m.value} onPress={() => setPayment(m.value)}>
              <Card
                style={{
                  padding: 12,
                  gap: 2,
                  borderColor: payment === m.value ? t.accent : t.border,
                  borderWidth: payment === m.value ? 2 : 1,
                }}
              >
                <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
                  <Ionicons
                    name={payment === m.value ? "radio-button-on" : "radio-button-off"}
                    size={18}
                    color={payment === m.value ? t.accent : t.textMuted}
                  />
                  <AppText weight="700" size={13}>{m.label}</AppText>
                </View>
                <AppText muted size={12}>{m.hint}</AppText>
              </Card>
            </Pressable>
          ))}
        </View>

        {/* Notes */}
        <TextField
          label="Order notes (optional)"
          placeholder="e.g. call before delivery"
          value={notes}
          onChangeText={setNotes}
          multiline
        />

        {/* Summary */}
        <Card style={{ gap: 8 }}>
          {items.map((i) => (
            <View key={i.id} style={{ flexDirection: "row", justifyContent: "space-between" }}>
              <AppText size={13} numberOfLines={1} style={{ flex: 1, marginRight: 8 }}>
                {i.productName} × {i.quantity}
              </AppText>
              <AppText size={13} weight="600">{inr(i.lineTotal)}</AppText>
            </View>
          ))}
          <View style={{ height: 1, backgroundColor: t.border }} />
          <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
            <AppText weight="700">Subtotal</AppText>
            <AppText weight="800" size={16}>{inr(cart.data?.subtotal)}</AppText>
          </View>
          <AppText muted size={12}>Delivery charges, taxes and coupon discount are finalised on the order.</AppText>
        </Card>

        <Button title="Place order" variant="accent" loading={placeOrder.isPending} onPress={onPlaceOrder} />
      </ScrollView>
    </Screen>
  );
}
