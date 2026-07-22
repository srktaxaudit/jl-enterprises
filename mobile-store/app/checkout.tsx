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
import { useCart, useEligibleCoupons, useValidateCoupon } from "@/features/cart/hooks";
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

// Mirrors OrderServiceImpl exactly (FREE_SHIPPING_THRESHOLD=5000, FLAT_SHIPPING=99),
// same as the web checkout — keep the three in sync if the rule ever changes.
const FREE_SHIPPING_THRESHOLD = 5000;
const FLAT_SHIPPING = 99;

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
  const [couponDiscount, setCouponDiscount] = useState(0);
  const [couponMessage, setCouponMessage] = useState<string | null>(null);
  const [notes, setNotes] = useState("");
  const eligible = useEligibleCoupons(status === "authed");

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

  const onApplyCoupon = (raw?: string) => {
    const code = (raw ?? couponCode).trim().toUpperCase();
    if (!code) return;
    if (raw) setCouponCode(code);
    validateCoupon.mutate(code, {
      onSuccess: (res) => {
        if (res.valid) {
          setAppliedCoupon(code);
          setCouponDiscount(res.discountAmount ?? 0);
          setCouponMessage(res.discountAmount ? `Coupon applied — you save ${inr(res.discountAmount)}.` : "Coupon applied.");
        } else {
          setAppliedCoupon(null);
          setCouponDiscount(0);
          setCouponMessage(res.message ?? "This coupon can't be applied.");
        }
      },
      onError: (e) => {
        setAppliedCoupon(null);
        setCouponDiscount(0);
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
                  setCouponDiscount(0);
                  setCouponMessage(null);
                }}
                autoCapitalize="characters"
              />
            </View>
            <Button title="Apply" variant="outline" loading={validateCoupon.isPending} onPress={() => onApplyCoupon()} />
          </View>
          {couponMessage ? (
            <AppText size={12} color={appliedCoupon ? t.success : t.danger}>{couponMessage}</AppText>
          ) : null}
          {(eligible.data ?? []).length > 0 ? (
            <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 6, marginTop: 4 }}>
              {(eligible.data ?? []).map((c) => (
                <Pressable key={c.code} onPress={() => onApplyCoupon(c.code)}>
                  <View
                    style={{
                      paddingHorizontal: 10, paddingVertical: 5, borderRadius: 999,
                      borderWidth: 1, borderStyle: "dashed",
                      borderColor: appliedCoupon === c.code ? t.success : t.accent,
                      backgroundColor: appliedCoupon === c.code ? t.success + "18" : "transparent",
                    }}
                  >
                    <AppText size={12} weight="700" color={appliedCoupon === c.code ? t.success : t.accent}>
                      {appliedCoupon === c.code ? "✓ " : "🏷 "}{c.code}
                    </AppText>
                  </View>
                </Pressable>
              ))}
            </View>
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

        {/* Summary — the same arithmetic the backend applies (subtotal − coupon, then
            free delivery at ₹5,000+, else flat ₹99), so the buyer commits to the REAL
            amount, not a subtotal with surprises added later. */}
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
          {(() => {
            const subtotal = cart.data?.subtotal ?? items.reduce((s, i) => s + (i.lineTotal ?? 0), 0);
            const discount = Math.min(couponDiscount, subtotal);
            const base = subtotal - discount;
            const shipping = base >= FREE_SHIPPING_THRESHOLD ? 0 : FLAT_SHIPPING;
            const grand = base + shipping;
            return (
              <View style={{ gap: 4 }}>
                <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
                  <AppText size={13} muted>Subtotal</AppText>
                  <AppText size={13} weight="600">{inr(subtotal)}</AppText>
                </View>
                {discount > 0 ? (
                  <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
                    <AppText size={13} color={t.success}>Coupon ({appliedCoupon})</AppText>
                    <AppText size={13} weight="600" color={t.success}>− {inr(discount)}</AppText>
                  </View>
                ) : null}
                <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
                  <AppText size={13} muted>Delivery</AppText>
                  <AppText size={13} weight="600" color={shipping === 0 ? t.success : undefined}>
                    {shipping === 0 ? "FREE" : inr(shipping)}
                  </AppText>
                </View>
                <View style={{ height: 1, backgroundColor: t.border }} />
                <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
                  <AppText weight="800" size={16}>Total to pay</AppText>
                  <AppText weight="800" size={18}>{inr(grand)}</AppText>
                </View>
                <AppText muted size={11}>Prices include GST. The order confirms this exact amount.</AppText>
              </View>
            );
          })()}
        </Card>

        <Button title="Place order" variant="accent" loading={placeOrder.isPending} onPress={onPlaceOrder} />
      </ScrollView>
    </Screen>
  );
}
