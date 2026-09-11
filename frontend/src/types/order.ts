import type { ApiResponse } from "@/types/auth";

export type ShippingAddress = { id:number; fullName:string; phone:string; address:string; city:string; defaultAddress:boolean; createdAt:string; updatedAt:string };
export type ShippingAddressInput = Omit<ShippingAddress,"id"|"createdAt"|"updatedAt">;
export type PaymentMethod = "cod" | "vnpay";
export type Payment = { id:number; method:PaymentMethod | "paypal"; status:"pending"|"completed"|"failed"; amount:number; referenceCode:string|null; transactionId:string|null; paidAt:string|null; paymentUrl:string|null };
export type PaymentMethodOptions = { codAvailable:boolean; vnpayAvailable:boolean };
export type CheckoutInput = { shippingAddressId:number; couponCode?:string; paymentMethod?:PaymentMethod };
export type CheckoutPreview = { subtotal:number; shippingFee:number; discountAmount:number; total:number; couponCode:string|null; discountDescription:string|null };
export type OrderItem = { id:number; productId:number; productSlug:string; productName:string; productUnit:string|null; productImage:string|null; quantity:number; unitPrice:number; lineTotal:number };
export type OrderHistory = { id:number; status:string; note:string|null; changedAt:string };
export type OrderSummary = { id:number; status:string; itemCount:number; total:number; recipientName:string; shippingCity:string; createdAt:string; deliveryStaffId:number|null; deliveryStaffName:string|null };
export type Order = { id:number; status:string; subtotal:number; shippingFee:number; discountAmount:number; total:number; couponCode:string|null; recipientName:string; recipientPhone:string; shippingAddress:string; shippingCity:string; items:OrderItem[]; statusHistory:OrderHistory[]; createdAt:string; updatedAt:string; payment:Payment|null; deliveryStaffId:number|null; deliveryStaffName:string|null; deliveryFailureReason:string|null };
export type OrderApiResponse<T> = ApiResponse<T>;
