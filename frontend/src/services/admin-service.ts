import { apiClient } from "@/lib/axios-client";
import type { AdminCoupon, AdminCouponFilters, AdminCouponInput, AdminOrder, AdminOrderFilters, AdminOrderSummary, AdminOrderStatus, AdminRefundRequest, AdminRefundRequestStatus, AdminRole, AdminUserFilters, AdminUserUpdate, CouponUsage } from "@/types/admin";
import type { ApiResponse, User } from "@/types/auth";
import type { PageData } from "@/types/catalog";

function compactParams(filters: object) {
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

export async function getAdminOrders(filters: AdminOrderFilters = {}) {
  const response = await apiClient.get<ApiResponse<PageData<AdminOrderSummary>>>("/admin/orders", { params: compactParams(filters) });
  return response.data.data;
}

export async function getAdminOrder(orderId: number) {
  const response = await apiClient.get<ApiResponse<AdminOrder>>(`/admin/orders/${orderId}`);
  return response.data.data;
}

export async function updateAdminOrderStatus(orderId: number, status: AdminOrderStatus, note?: string) {
  const response = await apiClient.patch<ApiResponse<AdminOrder>>(`/admin/orders/${orderId}/status`, { status, note });
  return response.data.data;
}

export async function assignAdminOrderDelivery(orderId: number, deliveryStaffId: number) {
  const response = await apiClient.patch<ApiResponse<AdminOrder>>(`/admin/orders/${orderId}/delivery-staff`, { deliveryStaffId });
  return response.data.data;
}

export async function getAdminRefundRequests(status?: AdminRefundRequestStatus) {
  const response = await apiClient.get<ApiResponse<PageData<AdminRefundRequest>>>("/admin/refund-requests", { params: compactParams({ status, size: 50, sort: "createdAt,desc" }) });
  return response.data.data;
}

export async function decideAdminRefundRequest(id: number, decision: "approve" | "reject", note?: string) {
  const response = await apiClient.patch<ApiResponse<AdminRefundRequest>>(`/admin/refund-requests/${id}/${decision}`, { note });
  return response.data.data;
}

export async function getAdminCoupons(filters: AdminCouponFilters = {}) {
  const response = await apiClient.get<ApiResponse<PageData<AdminCoupon>>>("/admin/coupons", { params: compactParams(filters) });
  return response.data.data;
}

export async function createAdminCoupon(input: AdminCouponInput) {
  const response = await apiClient.post<ApiResponse<AdminCoupon>>("/admin/coupons", input);
  return response.data.data;
}

export async function updateAdminCoupon(id: number, input: AdminCouponInput) {
  const response = await apiClient.put<ApiResponse<AdminCoupon>>(`/admin/coupons/${id}`, input);
  return response.data.data;
}

export async function updateAdminCouponActive(id: number, active: boolean) {
  const response = await apiClient.patch<ApiResponse<AdminCoupon>>(`/admin/coupons/${id}/active`, { active });
  return response.data.data;
}

export async function getAdminCouponUsages(id: number) {
  const response = await apiClient.get<ApiResponse<CouponUsage[]>>(`/admin/coupons/${id}/usages`);
  return response.data.data;
}

export async function deleteAdminCoupon(id: number) {
  await apiClient.delete(`/admin/coupons/${id}`);
}
