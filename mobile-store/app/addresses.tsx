import React, { useState } from "react";
import { Alert, FlatList, Pressable, ScrollView, View } from "react-native";
import { AppText, Button, Card, EmptyState, ErrorView, LoadingView, Screen, TextField } from "@/shared/components/ui";
import { SignInPrompt } from "@/shared/components/SignInPrompt";
import { useTheme } from "@/core/theme/ThemeProvider";
import { useAuth } from "@/core/auth/authStore";
import {
  useAddresses,
  useCreateAddress,
  useDeleteAddress,
  useSetDefaultAddress,
  useUpdateAddress,
} from "@/features/addresses/hooks";
import { ApiError } from "@/core/api/client";
import type { Address, AddressInput } from "@/core/types";

const EMPTY_FORM: AddressInput = {
  fullName: "",
  phone: "",
  line1: "",
  line2: "",
  city: "",
  state: "Tamil Nadu",
  postalCode: "",
  country: "India",
};

export default function Addresses() {
  const t = useTheme();
  const status = useAuth((s) => s.status);
  const addresses = useAddresses();
  const createAddress = useCreateAddress();
  const updateAddress = useUpdateAddress();
  const deleteAddress = useDeleteAddress();
  const setDefault = useSetDefaultAddress();

  const [editing, setEditing] = useState<Address | "new" | null>(null);
  const [form, setForm] = useState<AddressInput>(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  if (status === "guest") return <Screen><SignInPrompt message="Sign in to manage your delivery addresses." /></Screen>;
  if (addresses.isLoading) return <Screen><LoadingView label="Loading addresses…" /></Screen>;
  if (addresses.isError) {
    return <Screen><ErrorView message={(addresses.error as Error).message} onRetry={() => addresses.refetch()} /></Screen>;
  }

  const openNew = () => {
    setForm(EMPTY_FORM);
    setFieldErrors({});
    setEditing("new");
  };
  const openEdit = (a: Address) => {
    setForm({
      fullName: a.fullName ?? "",
      phone: a.phone ?? "",
      line1: a.line1,
      line2: a.line2 ?? "",
      city: a.city,
      state: a.state ?? "",
      postalCode: a.postalCode,
      country: a.country ?? "India",
      defaultAddress: a.defaultAddress,
    });
    setFieldErrors({});
    setEditing(a);
  };

  const onSave = () => {
    setFieldErrors({});
    const input: AddressInput = {
      ...form,
      fullName: form.fullName?.trim() || undefined,
      phone: form.phone?.trim() || undefined,
      line2: form.line2?.trim() || undefined,
      state: form.state?.trim() || undefined,
      country: form.country?.trim() || undefined,
    };
    const opts = {
      onSuccess: () => setEditing(null),
      onError: (e: unknown) => {
        if (e instanceof ApiError) {
          if (e.fieldErrors) setFieldErrors(e.fieldErrors);
          Alert.alert("Couldn't save address", e.message);
        } else {
          Alert.alert("Couldn't save address", "Please try again.");
        }
      },
    };
    if (editing === "new") createAddress.mutate(input, opts);
    else if (editing) updateAddress.mutate({ id: editing.id, input }, opts);
  };

  const onDelete = (a: Address) =>
    Alert.alert("Delete address?", "This can't be undone.", [
      { text: "Cancel", style: "cancel" },
      { text: "Delete", style: "destructive", onPress: () => deleteAddress.mutate(a.id) },
    ]);

  const field = (key: keyof AddressInput, label: string, placeholder: string, extra?: object) => (
    <TextField
      label={label}
      placeholder={placeholder}
      value={(form[key] as string) ?? ""}
      onChangeText={(v) => setForm((f) => ({ ...f, [key]: v }))}
      error={fieldErrors[key]}
      {...extra}
    />
  );

  if (editing) {
    return (
      <Screen>
        <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ paddingBottom: 24 }}>
          <AppText weight="800" size={17} style={{ marginBottom: 12 }}>
            {editing === "new" ? "Add address" : "Edit address"}
          </AppText>
          {field("fullName", "Full name", "Who receives the delivery")}
          {field("phone", "Phone", "Contact number for delivery", { keyboardType: "phone-pad" })}
          {field("line1", "Address line 1", "House / street")}
          {field("line2", "Address line 2 (optional)", "Area / landmark")}
          {field("city", "City", "Thoothukudi")}
          {field("state", "State", "Tamil Nadu")}
          {field("postalCode", "PIN code", "628001", { keyboardType: "number-pad" })}
          <View style={{ gap: 10 }}>
            <Button title="Save address" onPress={onSave} loading={createAddress.isPending || updateAddress.isPending} />
            <Button title="Cancel" variant="outline" onPress={() => setEditing(null)} />
          </View>
        </ScrollView>
      </Screen>
    );
  }

  const items = addresses.data ?? [];
  return (
    <Screen>
      {items.length === 0 ? (
        <EmptyState emoji="🏠" title="No addresses yet" hint="Add one so we know where to deliver." />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(a) => a.id}
          contentContainerStyle={{ paddingBottom: 12 }}
          renderItem={({ item }) => (
            <Card style={{ marginBottom: 10, gap: 4 }}>
              <AppText weight="700" size={14}>
                {item.fullName ?? "Address"}{item.defaultAddress ? " · Default" : ""}
              </AppText>
              <AppText muted size={13}>
                {[item.line1, item.line2, item.city, item.state, item.postalCode].filter(Boolean).join(", ")}
              </AppText>
              {item.phone ? <AppText muted size={13}>📞 {item.phone}</AppText> : null}
              <View style={{ flexDirection: "row", gap: 16, marginTop: 6 }}>
                <Pressable onPress={() => openEdit(item)} hitSlop={8}>
                  <AppText size={13} weight="700" color={t.accent}>Edit</AppText>
                </Pressable>
                {!item.defaultAddress ? (
                  <Pressable onPress={() => setDefault.mutate(item.id)} hitSlop={8}>
                    <AppText size={13} weight="700" color={t.primary}>Make default</AppText>
                  </Pressable>
                ) : null}
                <Pressable onPress={() => onDelete(item)} hitSlop={8}>
                  <AppText size={13} weight="700" color={t.danger}>Delete</AppText>
                </Pressable>
              </View>
            </Card>
          )}
        />
      )}
      <Button title="+ Add address" variant="accent" onPress={openNew} />
    </Screen>
  );
}
