import { apiClient } from "@/lib/axios-client";
import type { CheckoutInput, CheckoutPreview, Order, OrderApiResponse, OrderSummary, PaymentMethodOptions, RefundRequest, ShippingAddress, ShippingAddressInput } from "@/types/order";

export const addressService = {
  async findAll(){ return (await apiClient.get<OrderApiResponse<ShippingAddress[]>>("/shipping-addresses")).data.data; },
  async create(input:ShippingAddressInput){ return (await apiClient.post<OrderApiResponse<ShippingAddress>>("/shipping-addresses",input)).data.data; },
  async update(id:number,input:ShippingAddressInput){ return (await apiClient.put<OrderApiResponse<ShippingAddress>>(`/shipping-addresses/${id}`,input)).data.data; },
  async setDefault(id:number){ return (await apiClient.patch<OrderApiResponse<ShippingAddress>>(`/shipping-addresses/${id}/default`)).data.data; },
  async remove(id:number){ return (await apiClient.delete<OrderApiResponse<ShippingAddress[]>>(`/shipping-addresses/${id}`)).data.data; }
};

export const orderService = {
  async preview(input:CheckoutInput){ return (await apiClient.post<OrderApiResponse<CheckoutPreview>>("/checkout/preview",input)).data.data; },
  async create(input:CheckoutInput){ return (await apiClient.post<OrderApiResponse<Order>>("/orders",input)).data.data; },
  async findAll(){ return (await apiClient.get<OrderApiResponse<OrderSummary[]>>("/orders")).data.data; },
  async findOne(id:number){ return (await apiClient.get<OrderApiResponse<Order>>(`/orders/${id}`)).data.data; },
  async cancel(id:number){ return (await apiClient.patch<OrderApiResponse<Order>>(`/orders/${id}/cancel`)).data.data; },
  async getRefundRequest(id:number){ return (await apiClient.get<OrderApiResponse<RefundRequest | null>>(`/orders/${id}/refund-request`)).data.data; },
  async requestRefund(id:number,input:{reason:string;details?:string}){ return (await apiClient.post<OrderApiResponse<RefundRequest>>(`/orders/${id}/refund-requests`,input)).data.data; }
};

export const paymentService = {
  async methods(){ return (await apiClient.get<OrderApiResponse<PaymentMethodOptions>>("/payments/methods")).data.data; }
};
