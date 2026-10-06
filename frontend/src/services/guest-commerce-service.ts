import { apiClient } from "@/lib/axios-client";
import { getGuestSession, saveGuestSession } from "@/lib/guest-session";
import type { ApiResponse } from "@/types/auth";
import type { Cart, AddCartItemInput, UpdateCartItemInput } from "@/types/commerce";
import type { CheckoutPreview, GuestCheckoutInput, GuestOrderCreated, Order, PaymentMethodOptions } from "@/types/order";

async function token() {
  const current=getGuestSession(); if(current) return current.token;
  const response=await apiClient.post<ApiResponse<{token:string;expiresAt:string}>>("/public/guest/sessions");
  saveGuestSession(response.data.data); return response.data.data.token;
}
async function headers(){ return {"X-Guest-Token":await token()}; }

export const guestCommerceService={
  token,
  async getCart(){return (await apiClient.get<ApiResponse<Cart>>("/public/guest/cart",{headers:await headers()})).data.data;},
  async addItem(input:AddCartItemInput){return (await apiClient.post<ApiResponse<Cart>>("/public/guest/cart/items",input,{headers:await headers()})).data.data;},
  async updateItem(id:number,input:UpdateCartItemInput){return (await apiClient.patch<ApiResponse<Cart>>(`/public/guest/cart/items/${id}`,input,{headers:await headers()})).data.data;},
  async removeItem(id:number){return (await apiClient.delete<ApiResponse<Cart>>(`/public/guest/cart/items/${id}`,{headers:await headers()})).data.data;},
  async clear(){return (await apiClient.delete<ApiResponse<Cart>>("/public/guest/cart",{headers:await headers()})).data.data;},
  async preview(input:GuestCheckoutInput){return (await apiClient.post<ApiResponse<CheckoutPreview>>("/public/guest/checkout/preview",input,{headers:await headers()})).data.data;},
  async checkout(input:GuestCheckoutInput){return (await apiClient.post<ApiResponse<GuestOrderCreated>>("/public/guest/orders",input,{headers:await headers()})).data.data;},
  async recover(sessionToken:string,key:string){return (await apiClient.get<ApiResponse<GuestOrderCreated>>("/public/guest/orders/recovery",{headers:{"X-Guest-Token":sessionToken},params:{key}})).data.data;},
  async lookup(orderId:number,email:string,lookupToken:string){return (await apiClient.post<ApiResponse<Order>>("/public/guest/orders/lookup",{orderId,email,token:lookupToken})).data.data;},
  async paymentMethods(){return (await apiClient.get<ApiResponse<PaymentMethodOptions>>("/public/guest/payment-methods")).data.data;},
  async merge(){const current=getGuestSession();if(!current)return null;return (await apiClient.post<ApiResponse<Cart>>("/cart/merge-guest",null,{headers:{"X-Guest-Token":current.token}})).data.data;}
};
