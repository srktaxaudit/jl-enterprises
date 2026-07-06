import { create } from "zustand";
import { apiGet, apiPost, setUnauthorizedHandler } from "@/core/api/client";
import { tokenStore } from "./tokenStore";
import type { AuthTokens, AuthUser } from "@/core/types";

type Status = "loading" | "authed" | "guest";

interface AuthState {
  user: AuthUser | null;
  status: Status;
  login: (email: string, password: string) => Promise<void>;
  restore: () => Promise<void>;
  logout: () => Promise<void>;
}

export const useAuth = create<AuthState>((set) => ({
  user: null,
  status: "loading",

  async login(email, password) {
    const data = await apiPost<AuthTokens>("/api/v1/auth/login", { email, password });
    await tokenStore.set(data.accessToken, data.refreshToken);
    set({ user: data.user, status: "authed" });
  },

  async restore() {
    const access = await tokenStore.getAccess();
    if (!access) {
      set({ status: "guest", user: null });
      return;
    }
    try {
      const user = await apiGet<AuthUser>("/api/v1/auth/me");
      set({ user, status: "authed" });
    } catch {
      await tokenStore.clear();
      set({ status: "guest", user: null });
    }
  },

  async logout() {
    try {
      const refresh = await tokenStore.getRefresh();
      if (refresh) await apiPost("/api/v1/auth/logout", { refreshToken: refresh });
    } catch {
      /* best-effort */
    }
    await tokenStore.clear();
    set({ user: null, status: "guest" });
  },
}));

// When a token refresh fails deep in the API layer, drop to guest so the router
// redirects to the login screen.
setUnauthorizedHandler(() => {
  void tokenStore.clear();
  useAuth.setState({ user: null, status: "guest" });
});
