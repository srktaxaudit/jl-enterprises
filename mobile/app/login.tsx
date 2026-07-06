import { useState } from "react";
import { KeyboardAvoidingView, Platform, ScrollView, View } from "react-native";
import { useRouter } from "expo-router";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { AppText, Button, Screen, TextField } from "@/shared/components/ui";
import { useAuth } from "@/core/auth/authStore";
import { ApiError } from "@/core/api/client";
import { useTheme } from "@/core/theme/ThemeProvider";

const schema = z.object({
  email: z.string().min(1, "Email is required").email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});
type FormValues = z.infer<typeof schema>;

export default function Login() {
  const t = useTheme();
  const router = useRouter();
  const login = useAuth((s) => s.login);
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { email: "", password: "" } });

  const onSubmit = async (v: FormValues) => {
    setServerError(null);
    try {
      await login(v.email.trim(), v.password);
      router.replace("/(app)/");
    } catch (e) {
      setServerError(e instanceof ApiError ? e.message : "Login failed. Please try again.");
    }
  };

  return (
    <Screen>
      <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={{ flexGrow: 1, justifyContent: "center" }} keyboardShouldPersistTaps="handled">
          <View style={{ alignItems: "center", marginBottom: 28 }}>
            <View style={{ width: 64, height: 56, borderRadius: 14, backgroundColor: t.primary, alignItems: "center", justifyContent: "center" }}>
              <AppText color="#fff" weight="800" size={22}>JL</AppText>
            </View>
            <AppText weight="800" size={20} style={{ marginTop: 12 }}>JL Admin</AppText>
            <AppText muted size={13} style={{ marginTop: 2 }}>Sign in to manage the store</AppText>
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
          <Controller
            control={control}
            name="password"
            render={({ field: { onChange, onBlur, value } }) => (
              <TextField
                label="Password"
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                error={errors.password?.message}
                secureTextEntry
                autoCapitalize="none"
                placeholder="Your password"
              />
            )}
          />

          {serverError ? (
            <AppText color={t.danger} size={13} style={{ marginBottom: 10 }}>{serverError}</AppText>
          ) : null}

          <Button title="Sign in" onPress={handleSubmit(onSubmit)} loading={isSubmitting} />
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
