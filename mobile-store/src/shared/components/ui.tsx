import React from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  type TextInputProps,
  type ViewStyle,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useTheme } from "@/core/theme/ThemeProvider";

/** Full-screen container with safe-area + themed background. */
export function Screen({ children, padded = true }: { children: React.ReactNode; padded?: boolean }) {
  const t = useTheme();
  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: t.bg }} edges={["top", "left", "right"]}>
      <View style={{ flex: 1, padding: padded ? 16 : 0 }}>{children}</View>
    </SafeAreaView>
  );
}

export function AppText({
  children,
  muted,
  size = 15,
  weight = "400",
  color,
  style,
  numberOfLines,
}: {
  children: React.ReactNode;
  muted?: boolean;
  size?: number;
  weight?: "400" | "600" | "700" | "800";
  color?: string;
  style?: object;
  numberOfLines?: number;
}) {
  const t = useTheme();
  return (
    <Text
      numberOfLines={numberOfLines}
      style={[{ color: color ?? (muted ? t.textMuted : t.text), fontSize: size, fontWeight: weight }, style]}
    >
      {children}
    </Text>
  );
}

export function Card({ children, style }: { children: React.ReactNode; style?: ViewStyle }) {
  const t = useTheme();
  return (
    <View style={[styles.card, { backgroundColor: t.surface, borderColor: t.border }, style]}>{children}</View>
  );
}

export function Button({
  title,
  onPress,
  loading,
  disabled,
  variant = "primary",
}: {
  title: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: "primary" | "accent" | "outline" | "danger";
}) {
  const t = useTheme();
  const bg =
    variant === "accent" ? t.accent : variant === "danger" ? t.danger : variant === "outline" ? "transparent" : t.primary;
  const fg = variant === "outline" ? t.text : t.onPrimary;
  const isDisabled = disabled || loading;
  return (
    <Pressable
      onPress={onPress}
      disabled={isDisabled}
      style={({ pressed }) => [
        styles.btn,
        {
          backgroundColor: bg,
          borderColor: variant === "outline" ? t.border : bg,
          opacity: isDisabled ? 0.5 : pressed ? 0.85 : 1,
        },
      ]}
      accessibilityRole="button"
      accessibilityState={{ disabled: !!isDisabled, busy: !!loading }}
    >
      {loading ? <ActivityIndicator color={fg} /> : <Text style={{ color: fg, fontWeight: "700", fontSize: 15 }}>{title}</Text>}
    </Pressable>
  );
}

export function TextField({
  label,
  error,
  style,
  ...props
}: TextInputProps & { label?: string; error?: string }) {
  const t = useTheme();
  return (
    <View style={{ marginBottom: 14 }}>
      {label ? <AppText size={13} weight="600" muted style={{ marginBottom: 6 }}>{label}</AppText> : null}
      <TextInput
        placeholderTextColor={t.textMuted}
        style={[
          styles.input,
          { backgroundColor: t.surface, borderColor: error ? t.danger : t.border, color: t.text },
          style,
        ]}
        {...props}
      />
      {error ? <AppText size={12} color={t.danger} style={{ marginTop: 4 }}>{error}</AppText> : null}
    </View>
  );
}

export function StatusBadge({ label, tone = "info" }: { label: string; tone?: "info" | "success" | "warn" | "danger" | "muted" }) {
  const t = useTheme();
  const map = {
    info: t.primary,
    success: t.success,
    warn: t.warn,
    danger: t.danger,
    muted: t.textMuted,
  } as const;
  const c = (map as Record<string, string>)[tone] ?? t.textMuted;
  return (
    <View style={{ alignSelf: "flex-start", backgroundColor: c + "22", borderRadius: 999, paddingHorizontal: 10, paddingVertical: 3 }}>
      <Text style={{ color: c, fontSize: 11, fontWeight: "700" }}>{label}</Text>
    </View>
  );
}

export function LoadingView({ label = "Loading…" }: { label?: string }) {
  const t = useTheme();
  return (
    <View style={styles.center}>
      <ActivityIndicator color={t.accent} />
      <AppText muted size={13} style={{ marginTop: 10 }}>{label}</AppText>
    </View>
  );
}

export function EmptyState({ emoji = "📭", title, hint }: { emoji?: string; title: string; hint?: string }) {
  return (
    <View style={styles.center}>
      <Text style={{ fontSize: 40, marginBottom: 8 }}>{emoji}</Text>
      <AppText weight="700" size={16}>{title}</AppText>
      {hint ? <AppText muted size={13} style={{ marginTop: 4, textAlign: "center" }}>{hint}</AppText> : null}
    </View>
  );
}

export function ErrorView({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <ScrollView contentContainerStyle={styles.center}>
      <Text style={{ fontSize: 40, marginBottom: 8 }}>⚠️</Text>
      <AppText weight="700" size={16}>Couldn&apos;t load this</AppText>
      <AppText muted size={13} style={{ marginTop: 4, textAlign: "center" }}>{message}</AppText>
      {onRetry ? <View style={{ marginTop: 16, alignSelf: "stretch" }}><Button title="Retry" onPress={onRetry} variant="outline" /></View> : null}
    </ScrollView>
  );
}

/** − qty + stepper used in the cart and on the product page. */
export function QtyStepper({
  value,
  onChange,
  min = 1,
  max = 99,
}: {
  value: number;
  onChange: (next: number) => void;
  min?: number;
  max?: number;
}) {
  const t = useTheme();
  const StepBtn = ({ label, onPress, disabled }: { label: string; onPress: () => void; disabled: boolean }) => (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      accessibilityRole="button"
      style={({ pressed }) => [
        styles.stepBtn,
        { borderColor: t.border, backgroundColor: t.surfaceAlt, opacity: disabled ? 0.4 : pressed ? 0.7 : 1 },
      ]}
    >
      <Text style={{ color: t.text, fontSize: 18, fontWeight: "700" }}>{label}</Text>
    </Pressable>
  );
  return (
    <View style={{ flexDirection: "row", alignItems: "center", gap: 10 }}>
      <StepBtn label="−" onPress={() => onChange(value - 1)} disabled={value <= min} />
      <AppText weight="700" size={16} style={{ minWidth: 24, textAlign: "center" }}>{value}</AppText>
      <StepBtn label="+" onPress={() => onChange(value + 1)} disabled={value >= max} />
    </View>
  );
}

/** ★★★★☆-style rating row (read-only unless onRate is given). */
export function RatingStars({
  rating,
  size = 14,
  onRate,
}: {
  rating?: number | null;
  size?: number;
  onRate?: (value: number) => void;
}) {
  const t = useTheme();
  const r = Math.round(Number(rating ?? 0));
  return (
    <View style={{ flexDirection: "row", gap: 2 }}>
      {[1, 2, 3, 4, 5].map((i) => (
        <Pressable key={i} disabled={!onRate} onPress={() => onRate?.(i)} hitSlop={4}>
          <Text style={{ fontSize: size, color: i <= r ? t.warn : t.border }}>★</Text>
        </Pressable>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  card: { borderWidth: 1, borderRadius: 16, padding: 16 },
  btn: { minHeight: 48, borderRadius: 12, borderWidth: 1, alignItems: "center", justifyContent: "center", paddingHorizontal: 18 },
  input: { borderWidth: 1.5, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 12, fontSize: 15 },
  center: { flex: 1, alignItems: "center", justifyContent: "center", padding: 32 },
  stepBtn: { width: 36, height: 36, borderRadius: 10, borderWidth: 1, alignItems: "center", justifyContent: "center" },
});
