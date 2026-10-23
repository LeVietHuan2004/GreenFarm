import { apiClient } from "@/lib/axios-client";
import type { ApiResponse } from "@/types/auth";
import type { GhnLocation, Shipment, ShipmentEvent } from "@/types/order";

export const ghnService = {
  async status(){return (await apiClient.get<ApiResponse<{available:boolean}>>("/public/ghn/status")).data.data;},
  async provinces(){return (await apiClient.get<ApiResponse<GhnLocation[]>>("/public/ghn/provinces")).data.data;},
  async wards(provinceId:number){return (await apiClient.get<ApiResponse<GhnLocation[]>>("/public/ghn/wards",{params:{provinceId}})).data.data;},
  async create(orderId:number){return (await apiClient.post<ApiResponse<Shipment>>(`/admin/orders/${orderId}/ghn/create`)).data.data;},
  async sync(orderId:number){return (await apiClient.post<ApiResponse<Shipment>>(`/admin/orders/${orderId}/ghn/sync`)).data.data;},
  async cancel(orderId:number){return (await apiClient.post<ApiResponse<Shipment>>(`/admin/orders/${orderId}/ghn/cancel`)).data.data;},
  async printUrl(orderId:number){return (await apiClient.get<ApiResponse<{url:string}>>(`/admin/orders/${orderId}/ghn/print`)).data.data.url;},
  async events(orderId:number){return (await apiClient.get<ApiResponse<ShipmentEvent[]>>(`/admin/orders/${orderId}/ghn/events`)).data.data;}
};
