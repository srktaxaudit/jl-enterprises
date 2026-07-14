import { create } from "zustand";
import { apiGet, apiPost, apiPut, setUnauthorizedHandler } from "@/core/api/client";
import { tokenStore } from "./tokenStore";
import type { AuthTokens, AuthUser } from "@/core/types";

// Unlike the admin app, guests can browse the catalog freely — auth is only
// needed for the cart, wishlist, checkout and order history.
type Status = "loading" | "authed" | "guest";

export interface RegisterInput {
  email: string;
  password: string;
  firstName: string;
  lastName?: string;
  phone?: string;
  whatsappOptIn?: boolean;
}

interface AuthState {
  user: AuthUser | null;
  status: Status;
  login: (email: string, password: string) => Promise<void>;
  register: (input: RegisterInput) => Promise<void>;
  restore: () => Promise<void>;
  logout: () => Promise<void>;
  updateProfile: (input: Partial<Pick<AuthUser, "firstName" | "lastName" | "phone" | "whatsappOptIn">>) => Promise<void>;
}

export const useAuth = create<AuthState>((set) => ({
  user: null,
  status: "loading",

  async login(email, password) {
    const data = await apiPost<AuthTokens>("/api/v1/auth/login", { email, password });
    await tokenStore.set(data.accessToken, data.refreshToken);
    set({ user: data.user, status: "authed" });
  },

  async register(input) {
    const data = await apiPost<AuthTokens>("/api/v1/auth/register", input);
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

  async updateProfile(input) {
    const user = await apiPut<AuthUser>("/api/v1/auth/me", input);
    set({ user });
  },
}));

// When a token refresh fails deep in the API layer, drop to guest — the
// customer can keep browsing and is asked to sign in again at checkout.
setUnauthorizedHandler(() => {
  void tokenStore.clear();
  useAuth.setState({ user: null, status: "guest" });
});
