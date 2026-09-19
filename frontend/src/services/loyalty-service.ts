import { apiClient } from "@/lib/axios-client";
import type { LoyaltyApiResponse, LoyaltySummary } from "@/types/loyalty";

export async function getLoyaltySummary() {
  return (await apiClient.get<LoyaltyApiResponse<LoyaltySummary>>("/loyalty")).data.data;
}
