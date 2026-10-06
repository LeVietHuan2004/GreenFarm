import { apiClient } from "@/lib/axios-client";
import type { ApiResponse } from "@/types/auth";
import type { AvailableCoupon } from "@/types/coupon";

export async function getAvailableCoupons(): Promise<AvailableCoupon[]> {
  return (await apiClient.get<ApiResponse<AvailableCoupon[]>>("/public/coupons")).data.data;
}
