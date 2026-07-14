import { useState } from "react";
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, View } from "react-native";
import { useRouter } from "expo-router";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { AppText, Button, Screen, TextField } from "@/shared/components/ui";
import { useAuth } from "@/core/auth/authStore";
import { ApiError } from "@/core/api/client";
import { useTheme } from "@/core/theme/ThemeProvider";

const schema = z.object({
  firstName: z.string().min(1, "First name is required").max(80),
  lastName: z.string().max(80).optional(),
  email: z.string().min(1, "Email is required").email("Enter a valid email"),
  phone: z
    .string()
    .optional()
    .refine((v) => !v || /^[0-9+\-() ]{8,20}$/.test(v), "Enter a valid mobile number"),
  password: z.string().min(8, "At least 8 characters"),
});
type FormValues = z.infer<typeof schema>;

export default function Signup() {
  const t = useTheme();
  const router = useRouter();
  const register = useAuth((s) => s.register);
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { firstName: "", lastName: "", email: "", phone: "", password: "" },
  });

  const onSubmit = async (v: FormValues) => {
    setServerError(null);
    try {
      await register({
        email: v.email.trim(),
        password: v.password,
        firstName: v.firstName.trim(),
        lastName: v.lastName?.trim() || undefined,
        phone: v.phone?.trim() || undefined,
      });
      router.replace("/(tabs)");
    } catch (e) {
      if (e instanceof ApiError) {
        // Surface field-level validation errors inline where possible.
        for (const [field, message] of Object.entries(e.fieldErrors ?? {})) {
          if (field in v) setError(field as keyof FormValues, { message });
        }
        setServerError(e.message);
      } else {
        setServerError("Sign-up failed. Please try again.");
      }
    }
  };

  return (
    <Screen>
      <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={{ flexGrow: 1, justifyContent: "center" }} keyboardShouldPersistTaps="handled">
          <View style={{ alignItems: "center", marginBottom: 24 }}>
            <View style={{ width: 64, height: 56, borderRadius: 14, backgroundColor: t.primary, alignItems: "center", justifyContent: "center" }}>
              <AppText color="#fff" weight="800" size={22}>JL</AppText>
            </View>
            <AppText weight="800" size={20} style={{ marginTop: 12 }}>Create your account</AppText>
            <AppText muted size={13} style={{ marginTop: 2 }}>Shop appliances & furniture from JL Enterprises</AppText>
          </View>

          <Controller
            control={control}
            name="firstName"
            render={({ field: { onChange, onBlur, value } }) => (
              <TextField label="First name" value={value} onChangeText={onChange} onBlur={onBlur} error={errors.firstName?.message} placeholder="Priya" />
            )}
          />
          <Controller
            control={control}
            name="lastName"
            render={({ field: { onChange, onBlur, value } }) => (
              <TextField label="Last name (optional)" value={value ?? ""} onChangeText={onChange} onBlur={onBlur} error={errors.lastName?.message} placeholder="Kumar" />
            )}
          />
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
            name="phone"
            render={({ field: { onChange, onBlur, value } }) => (
              <TextField
                label="Mobile number (optional)"
                value={value ?? ""}
                onChangeText={onChange}
                onBlur={onBlur}
                error={errors.phone?.message}
                keyboardType="phone-pad"
                placeholder="98xxxxxxxx"
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
                placeholder="At least 8 characters"
              />
            )}
          />

          {serverError ? (
            <AppText color={t.danger} size={13} style={{ marginBottom: 10 }}>{serverError}</AppText>
          ) : null}

          <Button title="Create account" onPress={handleSubmit(onSubmit)} loading={isSubmitting} />
          <Pressable onPress={() => router.push("/login")} style={{ marginTop: 18, alignItems: "center", marginBottom: 24 }} hitSlop={8}>
            <AppText size={13} muted>
              Already have an account? <AppText size={13} weight="700" color={t.accent}>Sign in</AppText>
            </AppText>
          </Pressable>
        </ScrollView>
      </KeyboardAvoidingView>
    </Screen>
  );
}
