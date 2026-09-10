import axios from "axios";

import { useAuthStore } from "@/stores/auth-store";

export const apiClient = axios.create({
  baseURL:
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api",
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

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url as string | undefined;
    const isAuthRequest = requestUrl?.startsWith("/auth/");
    const currentToken = useAuthStore.getState().token;
    const belongsToCurrentSession = currentToken && error.config?.headers?.Authorization === `Bearer ${currentToken}`;
    if (error.response?.status === 401 && !isAuthRequest && belongsToCurrentSession) {
      useAuthStore.getState().clearSession();
    }
    return Promise.reject(error);
  }
);
