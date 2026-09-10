import { apiClient } from "@/lib/axios-client";
import type {
  ApiResponse,
  AuthSession,
  ChangePasswordInput,
  LoginInput,
  RegisterInput,
  UpdateProfileInput,
  User
} from "@/types/auth";

export const authService = {
  async login(input: LoginInput): Promise<AuthSession> {
    const response = await apiClient.post<ApiResponse<AuthSession>>(
      "/auth/login",
      input
    );
    return response.data.data;
  },

  async register(input: RegisterInput): Promise<AuthSession> {
    const response = await apiClient.post<ApiResponse<AuthSession>>(
      "/auth/register",
      input
    );
    return response.data.data;
  },

  async getProfile(): Promise<User> {
    const response = await apiClient.get<ApiResponse<User>>("/users/me");
    return response.data.data;
  },

  async updateProfile(input: UpdateProfileInput): Promise<User> {
    const response = await apiClient.patch<ApiResponse<User>>("/users/me", input);
    return response.data.data;
  },

  async changePassword(input: ChangePasswordInput): Promise<void> {
    await apiClient.put("/users/me/password", input);
  }
};
