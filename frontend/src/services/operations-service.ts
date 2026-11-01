import { apiClient } from "@/lib/axios-client";
import type { Order, OrderApiResponse } from "@/types/order";
import type { ApiResponse } from "@/types/auth";
import type { InventoryAdjustment, InventoryBatch, InventoryDetail, InventoryPage, InventoryProduct } from "@/types/inventory";

export const staffOrderService = {
  async findAll() { return (await apiClient.get<OrderApiResponse<Order[]>>("/staff/orders")).data.data; },
  async updateStatus(id: number, status: string, note?: string) {
    return (await apiClient.patch<OrderApiResponse<Order>>(`/staff/orders/${id}/status`, { status, note })).data.data;
  },
  async deliveryStaff() { return (await apiClient.get<ApiResponse<{id:number;name:string}[]>>("/staff/orders/delivery-staff")).data.data; },
  async assign(id:number, deliveryStaffId:number) { return (await apiClient.patch<OrderApiResponse<Order>>(`/staff/orders/${id}/delivery-staff`, {deliveryStaffId})).data.data; }
};

export const staffInventoryService = {
  async list(search:string, page:number) { return (await apiClient.get<ApiResponse<InventoryPage<InventoryProduct>>>("/staff/inventory", {params:{search, page, size:20}})).data.data; },
  async detail(id:number) { return (await apiClient.get<ApiResponse<InventoryDetail>>(`/staff/inventory/${id}`)).data.data; },
  async adjust(input:InventoryAdjustment) { return (await apiClient.post<ApiResponse<InventoryBatch>>("/staff/inventory/adjust", input)).data.data; }
};

export const deliveryOrderService = {
  async findAll() { return (await apiClient.get<OrderApiResponse<Order[]>>("/delivery/orders")).data.data; },
  async claim(id: number) { return (await apiClient.post<OrderApiResponse<Order>>(`/delivery/orders/${id}/claim`)).data.data; },
  async updateStatus(id: number, status: string, note?: string) {
    return (await apiClient.patch<OrderApiResponse<Order>>(`/delivery/orders/${id}/status`, { status, note })).data.data;
  }
};
