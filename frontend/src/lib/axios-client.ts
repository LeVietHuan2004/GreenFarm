import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";

import { useAuthStore } from "@/stores/auth-store";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    "Content-Type": "application/json"
  },
  timeout: 10_000
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

type RetryConfig = InternalAxiosRequestConfig & { _retry?: boolean };
let refreshPromise: Promise<string> | null = null;

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetryConfig | undefined;
    const requestUrl = error.config?.url as string | undefined;
    const isAuthRequest = requestUrl?.startsWith("/auth/");
    const state = useAuthStore.getState();
    const currentToken = state.token;
    const belongsToCurrentSession = currentToken && error.config?.headers?.Authorization === `Bearer ${currentToken}`;
    if (error.response?.status === 401 && !isAuthRequest && belongsToCurrentSession && config && !config._retry && state.refreshToken) {
      config._retry = true;
      refreshPromise ??= axios.post(`${apiBaseUrl}/auth/refresh`, { refreshToken: state.refreshToken }).then((response) => {
        const session = response.data.data;
        useAuthStore.getState().setSession(session);
        return session.accessToken as string;
      }).finally(() => { refreshPromise = null; });
      try {
        const accessToken = await refreshPromise;
        config.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(config);
      } catch {
        useAuthStore.getState().clearSession();
      }
    } else if (error.response?.status === 401 && !isAuthRequest && belongsToCurrentSession) {
      useAuthStore.getState().clearSession();
    }
    return Promise.reject(error);
  }
);
