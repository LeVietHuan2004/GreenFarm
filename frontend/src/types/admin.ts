import type { User, UserRole } from "@/types/auth";
import type { Order, OrderSummary } from "@/types/order";

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

export type AdminOrderStatus = "pending" | "processing" | "ready_for_delivery" | "out_for_delivery" | "delivered" | "completed" | "canceled";
export type AdminOrder = Order;
export type AdminOrderSummary = OrderSummary;
export type AdminOrderFilters = { status?: AdminOrderStatus; page?: number; size?: number; sort?: string };

export type CouponType = "ORDER_DISCOUNT" | "FREESHIP";
export type DiscountType = "PERCENTAGE" | "FIXED_AMOUNT";
export type AdminCoupon = {
  id: number;
  code: string;
  couponType: CouponType;
  discountType: DiscountType;
  discountPercentage: number;
  discountAmount: number | null;
  startsAt: string | null;
  expiresAt: string | null;
  usageLimit: number | null;
  timesUsed: number;
  active: boolean;
  currentlyUsable: boolean;
  createdAt: string;
  updatedAt: string;
};
export type AdminCouponInput = {
  code: string;
  couponType: CouponType;
  discountType: DiscountType;
  discountPercentage: number;
  discountAmount: number | null;
  startsAt: string | null;
  expiresAt: string | null;
  usageLimit: number | null;
  active: boolean;
};
export type AdminCouponFilters = { search?: string; active?: boolean; page?: number; size?: number; sort?: string };
