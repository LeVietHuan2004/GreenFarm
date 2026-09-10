import { apiClient } from "@/lib/axios-client";
import type {
  AddCartItemInput,
  Cart,
  CommerceApiResponse,
  MergeCartInput,
  UpdateCartItemInput
} from "@/types/commerce";

export const cartService = {
  async getCart(): Promise<Cart> {
    const response = await apiClient.get<CommerceApiResponse<Cart>>("/cart");
    return response.data.data;
  },

  async addItem(input: AddCartItemInput): Promise<Cart> {
    const response = await apiClient.post<CommerceApiResponse<Cart>>("/cart/items", input);
    return response.data.data;
  },

  async updateItem(itemId: number, input: UpdateCartItemInput): Promise<Cart> {
    const response = await apiClient.patch<CommerceApiResponse<Cart>>(
      `/cart/items/${itemId}`,
      input
    );
    return response.data.data;
  },

  async removeItem(itemId: number): Promise<Cart> {
    const response = await apiClient.delete<CommerceApiResponse<Cart>>(
      `/cart/items/${itemId}`
    );
    return response.data.data;
  },

  async merge(input: MergeCartInput): Promise<Cart> {
    const response = await apiClient.post<CommerceApiResponse<Cart>>("/cart/merge", input);
    return response.data.data;
  },

  async clear(): Promise<Cart> {
    const response = await apiClient.delete<CommerceApiResponse<Cart>>("/cart");
    return response.data.data;
  }
};
