import type { ApiResponse } from "@/types/auth";

export type LoyaltyTransaction = { id:number; type:"order_earn"|"review_earn"|"redemption"|"redemption_restore"; points:number; description:string; createdAt:string };
export type LoyaltySummary = { pointsBalance:number; discountPerPoint:number; earnAmountPerPoint:number; maxRedemptionPercent:number; transactions:LoyaltyTransaction[] };
export type LoyaltyApiResponse<T> = ApiResponse<T>;
