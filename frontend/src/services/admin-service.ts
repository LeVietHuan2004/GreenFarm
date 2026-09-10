import { apiClient } from "@/lib/axios-client";
import type { AdminRole, AdminUserFilters, AdminUserUpdate } from "@/types/admin";
import type { ApiResponse, User } from "@/types/auth";
import type { PageData } from "@/types/catalog";

function compactParams(filters: AdminUserFilters) {
  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== undefined && value !== "")
  );
}

export async function getAdminUsers(filters: AdminUserFilters = {}) {
  const response = await apiClient.get<ApiResponse<PageData<User>>>("/admin/users", {
    params: compactParams(filters)
  });
  return response.data.data;
}

export async function getAdminRoles() {
  const response = await apiClient.get<ApiResponse<AdminRole[]>>("/roles");
  return response.data.data;
}

export async function updateAdminUser(userId: number, input: AdminUserUpdate) {
  const response = await apiClient.patch<ApiResponse<User>>(`/admin/users/${userId}`, input);
  return response.data.data;
}
