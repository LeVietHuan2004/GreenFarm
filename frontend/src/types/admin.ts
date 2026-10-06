import type { User, UserRole } from "@/types/auth";
import type { Order, OrderSummary } from "@/types/order";
import type { RefundRequest, RefundRequestStatus } from "@/types/order";

export type AdminUserStatus = User["status"];

export type AdminUserFilters = {
  search?: string;
  role?: UserRole;
  status?: AdminUserStatus;
  page?: number;
  size?: number;
  sort?: string;
};

export type AdminUserUpdate = {
  role?: UserRole;
  status?: AdminUserStatus;
};

export type AdminRole = {
  id: number;
  name: UserRole;
  permissions: string[];
};

export type AdminOrderStatus = "pending" | "processing" | "ready_for_delivery" | "out_for_delivery" | "delivered" | "delivery_failed" | "completed" | "canceled";
export type AdminOrder = Order;
export type AdminOrderSummary = OrderSummary;
export type AdminOrderView = "active" | "history" | "all";
export type AdminOrderFilters = { view?: AdminOrderView; status?: AdminOrderStatus; page?: number; size?: number; sort?: string };
export type AdminRefundRequest = RefundRequest;
export type AdminRefundRequestStatus = RefundRequestStatus;

export type CouponType = "ORDER_DISCOUNT" | "FREESHIP";
export type DiscountType = "PERCENTAGE" | "FIXED_AMOUNT";
export type CouponScopeType = "ALL" | "CATEGORY" | "PRODUCT";
export type AdminCoupon = {
  id: number;
  code: string;
  name: string;
  description: string | null;
  couponType: CouponType;
  discountType: DiscountType;
  discountPercentage: number;
  discountAmount: number | null;
  maxDiscountAmount: number | null;
  minimumOrderAmount: number | null;
  scopeType: CouponScopeType;
  categoryIds: number[];
  productIds: number[];
  startsAt: string | null;
  expiresAt: string | null;
  usageLimit: number | null;
  usageLimitPerUser: number | null;
  timesUsed: number;
  active: boolean;
  currentlyUsable: boolean;
  createdAt: string;
  updatedAt: string;
};
export type AdminCouponInput = {
  code: string;
  name: string;
  description: string | null;
  couponType: CouponType;
  discountType: DiscountType;
  discountPercentage: number;
  discountAmount: number | null;
  maxDiscountAmount: number | null;
  minimumOrderAmount: number | null;
  scopeType: CouponScopeType;
  categoryIds: number[];
  productIds: number[];
  startsAt: string | null;
  expiresAt: string | null;
  usageLimit: number | null;
  usageLimitPerUser: number | null;
  active: boolean;
};
export type AdminCouponFilters = { search?: string; active?: boolean; page?: number; size?: number; sort?: string };
export type CouponUsage = { id:number; userId:number; userName:string; orderId:number; discountAmount:number; status:"RESERVED"|"USED"|"RELEASED"; createdAt:string; usedAt:string|null; releasedAt:string|null };
