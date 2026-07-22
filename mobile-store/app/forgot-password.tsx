import { useState } from "react";
import { KeyboardAvoidingView, Platform, ScrollView, View } from "react-native";
import { Stack, useRouter } from "expo-router";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { AppText, Button, Screen, TextField } from "@/shared/components/ui";
import { apiPost, ApiError } from "@/core/api/client";
import { useTheme } from "@/core/theme/ThemeProvider";

const schema = z.object({
  email: z.string().min(1, "Email is required").email("Enter a valid email address"),
});
type FormValues = z.infer<typeof schema>;

/**
 * Requests a password-reset email (POST /api/v1/auth/forgot-password). The email's
 * link opens the storefront's reset page where the new password is set — the same
 * flow the website uses. The endpoint deliberately always reports success so it
 * never reveals whether an account exists.
 */
export default function ForgotPassword() {
  const t = useTheme();
  const router = useRouter();
  const [sent, setSent] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { email: "" } });

  const onSubmit = async (v: FormValues) => {
    setServerError(null);
    try {
      await apiPost<void>("/api/v1/auth/forgot-password", { email: v.email.trim() });
      setSent(true);
    } catch (e) {
      setServerError(e instanceof ApiError ? e.message : "Something went wrong. Please try again.");
    }
  };

  return (
    <Screen>
      <Stack.Screen options={{ title: "Reset password" }} />
      <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={{ flexGrow: 1, justifyContent: "center" }} keyboardShouldPersistTaps="handled">
          {sent ? (
            <View style={{ alignItems: "center", gap: 10 }}>
              <AppText size={40}>📬</AppText>
              <AppText weight="800" size={18}>Check your email</AppText>
              <AppText muted size={13} style={{ textAlign: "center" }}>
                If an account exists for that address, we've sent a link to reset your password.
                Open it on this phone or any browser, set a new password, then sign in here.
              </AppText>
              <View style={{ marginTop: 12, alignSelf: "stretch" }}>
                <Button title="Back to sign in" onPress={() => router.back()} />
              </View>
            </View>
          ) : (
            <>
              <View style={{ alignItems: "center", marginBottom: 24 }}>
                <AppText weight="800" size={20}>Forgot your password?</AppText>
                <AppText muted size={13} style={{ marginTop: 4, textAlign: "center" }}>
                  Enter your account email and we'll send you a reset link.
                </AppText>
              </View>
              <Controller
                control={control}
                name="email"
                render={({ field: { onChange, onBlur, value } }) => (
                  <TextField
                    label="Email"
                    value={value}
                    onChangeText={onChange}
                    onBlur={onBlur}
                    error={errors.email?.message}
                    autoCapitalize="none"
                    keyboardType="email-address"
                    autoComplete="email"
                    placeholder="you@example.com"
                  />
                )}
              />
              {serverError ? (
                <AppText color={t.danger} size={13} style={{ marginBottom: 10 }}>{serverError}</AppText>
              ) : null}
              <Button title="Send reset link" onPress={handleSubmit(onSubmit)} loading={isSubmitting} />
            </>
          )}
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
