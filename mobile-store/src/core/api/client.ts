import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { API_BASE_URL } from "@/core/config/env";
import { tokenStore } from "@/core/auth/tokenStore";
import type { ApiResponse } from "@/core/types";

/** Normalised API error surfaced to the UI (message + optional field errors). */
export class ApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;
  constructor(message: string, status: number, fieldErrors?: Record<string, string>) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

// A raw client WITHOUT interceptors, used for the refresh call so a 401 there
// can't recurse back into the refresh flow.
const raw = axios.create({ baseURL: API_BASE_URL, timeout: 20000 });

export const http = axios.create({ baseURL: API_BASE_URL, timeout: 20000 });

// Set by the auth layer so a failed refresh can drop the user to guest.
let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn;
}

http.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  const token = await tokenStore.getAccess();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Single-flight refresh: concurrent 401s share one refresh request.
let refreshing: Promise<string | null> | null = null;
async function refreshAccess(): Promise<string | null> {
  const refresh = await tokenStore.getRefresh();
  if (!refresh) return null;
  try {
    const res = await raw.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
      "/api/v1/auth/refresh",
      { refreshToken: refresh },
    );
    const data = res.data.data;
    await tokenStore.set(data.accessToken, data.refreshToken ?? refresh);
    return data.accessToken;
  } catch {
    return null;
  }
}

http.interceptors.response.use(
  (res) => res,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    const status = error.response?.status ?? 0;

    if (status === 401 && original && !original._retried) {
      original._retried = true;
      refreshing = refreshing ?? refreshAccess();
      const newToken = await refreshing;
      refreshing = null;
      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`;
        return http.request(original);
      }
      // Only a signed-in session can expire; guests never had a refresh token.
      if (await tokenStore.getRefresh()) {
        await tokenStore.clear();
        onUnauthorized?.();
      }
    }

    const body = error.response?.data;
    const message = body?.message || error.message || "Something went wrong. Please try again.";
    const fieldErrors =
      body?.data && typeof body.data === "object" && !Array.isArray(body.data)
        ? (body.data as Record<string, string>)
        : undefined;
    return Promise.reject(new ApiError(message, status, fieldErrors));
  },
);

// ── Typed helpers: unwrap the { success, message, data } envelope ──
export async function apiGet<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await http.get<ApiResponse<T>>(url, { params });
  return res.data.data;
}
export async function apiPost<T>(url: string, body?: unknown, params?: Record<string, unknown>): Promise<T> {
  const res = await http.post<ApiResponse<T>>(url, body, { params });
  return res.data.data;
}
export async function apiPut<T>(url: string, body?: unknown, params?: Record<string, unknown>): Promise<T> {
  const res = await http.put<ApiResponse<T>>(url, body, { params });
  return res.data.data;
}
export async function apiDelete<T>(url: string): Promise<T> {
  const res = await http.delete<ApiResponse<T>>(url);
  return res.data.data;
}
