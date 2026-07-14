import * as SecureStore from "expo-secure-store";

// Tokens live in the OS keychain / keystore (encrypted at rest) — never in
// AsyncStorage. Keys are namespaced so they never collide with the admin app.
const ACCESS = "jl_store_access";
const REFRESH = "jl_store_refresh";

export const tokenStore = {
  getAccess: () => SecureStore.getItemAsync(ACCESS),
  getRefresh: () => SecureStore.getItemAsync(REFRESH),
  async set(access: string, refresh: string) {
    await SecureStore.setItemAsync(ACCESS, access);
    await SecureStore.setItemAsync(REFRESH, refresh);
  },
  async clear() {
    await SecureStore.deleteItemAsync(ACCESS);
    await SecureStore.deleteItemAsync(REFRESH);
  },
};
