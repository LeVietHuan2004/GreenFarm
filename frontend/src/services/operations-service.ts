import { apiClient } from "@/lib/axios-client";
import type { Order, OrderApiResponse } from "@/types/order";

export const staffOrderService = {
  async findAll() { return (await apiClient.get<OrderApiResponse<Order[]>>("/staff/orders")).data.data; },
  async updateStatus(id: number, status: string, note?: string) {
    return (await apiClient.patch<OrderApiResponse<Order>>(`/staff/orders/${id}/status`, { status, note })).data.data;
  }
};

export const deliveryOrderService = {
  async findAll() { return (await apiClient.get<OrderApiResponse<Order[]>>("/delivery/orders")).data.data; },
  async claim(id: number) { return (await apiClient.post<OrderApiResponse<Order>>(`/delivery/orders/${id}/claim`)).data.data; },
  async updateStatus(id: number, status: string, note?: string) {
    return (await apiClient.patch<OrderApiResponse<Order>>(`/delivery/orders/${id}/status`, { status, note })).data.data;
  }
};
