import type { ApiResponse } from "@/types/auth";

export type ShippingAddress = { id:number; fullName:string; phone:string; address:string; city:string; defaultAddress:boolean; createdAt:string; updatedAt:string };
export type ShippingAddressInput = Omit<ShippingAddress,"id"|"createdAt"|"updatedAt">;
export type PaymentMethod = "cod" | "vnpay";
export type Payment = { id:number; method:PaymentMethod | "paypal"; status:"pending"|"completed"|"failed"|"refunded"; amount:number; referenceCode:string|null; transactionId:string|null; paidAt:string|null; paymentUrl:string|null };
export type PaymentMethodOptions = { codAvailable:boolean; vnpayAvailable:boolean };
export type CheckoutInput = { shippingAddressId:number; couponCode?:string; freeShippingCouponCode?:string; paymentMethod?:PaymentMethod; loyaltyPoints?:number };
export type GuestCheckoutInput = { name:string; phone:string; email:string; shippingAddress:string; shippingCity:string; shippingMethod:"standard"|"express"; couponCode?:string; freeShippingCouponCode?:string; paymentMethod?:PaymentMethod; idempotencyKey:string };
export type CheckoutPreview = { subtotal:number; shippingFee:number; discountAmount:number; loyaltyDiscountAmount:number; loyaltyPointsApplied:number; shippingDiscountAmount:number; total:number; couponCode:string|null; freeShippingCouponCode:string|null; discountDescription:string|null; shippingDiscountDescription:string|null };
export type OrderItem = { id:number; productId:number; productSlug:string; productName:string; productUnit:string|null; productImage:string|null; quantity:number; unitPrice:number; lineTotal:number };
export type OrderHistory = { id:number; status:string; note:string|null; changedAt:string };
export type RefundRequestStatus = "pending" | "processing" | "approved" | "rejected";
export type RefundRequest = { id:number; orderId:number; orderStatus:string; customerName:string; reason:string; details:string|null; status:RefundRequestStatus; adminNote:string|null; reviewedBy:string|null; reviewedAt:string|null; createdAt:string };
export type OrderSummary = { id:number; status:string; itemCount:number; total:number; recipientName:string; shippingCity:string; createdAt:string; deliveryStaffId:number|null; deliveryStaffName:string|null; deliveryClaimedAt:string|null };
export type Order = { id:number; status:string; subtotal:number; shippingFee:number; discountAmount:number; total:number; couponCode:string|null; recipientName:string; recipientPhone:string; shippingAddress:string; shippingCity:string; items:OrderItem[]; statusHistory:OrderHistory[]; createdAt:string; updatedAt:string; payment:Payment|null; deliveryStaffId:number|null; deliveryStaffName:string|null; deliveryClaimedAt:string|null; deliveryFailureReason:string|null; loyaltyPointsUsed:number; loyaltyDiscountAmount:number; shippingCouponCode:string|null; shippingDiscountAmount:number };
export type OrderApiResponse<T> = ApiResponse<T>;
export type GuestOrderCreated = { order:Order; lookupToken:string; replayed:boolean; paymentUrl:string|null };
