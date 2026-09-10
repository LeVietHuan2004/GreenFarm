import { apiClient } from "@/lib/axios-client";
import type { CommerceApiResponse, Wishlist } from "@/types/commerce";

export const wishlistService = {
  async getWishlist(): Promise<Wishlist> {
    const response = await apiClient.get<CommerceApiResponse<Wishlist>>("/wishlist");
    return response.data.data;
  },

  async addItem(productId: number): Promise<Wishlist> {
    const response = await apiClient.post<CommerceApiResponse<Wishlist>>(
      "/wishlist/items",
      { productId }
    );
    return response.data.data;
  },

  async removeItem(productId: number): Promise<Wishlist> {
    const response = await apiClient.delete<CommerceApiResponse<Wishlist>>(
      `/wishlist/items/${productId}`
    );
    return response.data.data;
  }
};
