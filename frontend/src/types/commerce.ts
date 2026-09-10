import type { ApiResponse } from "@/types/auth";
import type { Product } from "@/types/catalog";

export type CartItem = {
  id: number;
  quantity: number;
  lineTotal: number;
  product: Product;
  createdAt: string;
  updatedAt: string;
};

export type Cart = {
  items: CartItem[];
  totalItems: number;
  subtotal: number;
};

export type AddCartItemInput = {
  productId: number;
  quantity: number;
};

export type UpdateCartItemInput = {
  quantity: number;
};

export type MergeCartInput = {
  items: AddCartItemInput[];
};

export type WishlistItem = {
  id: number;
  product: Product;
  createdAt: string;
};

export type Wishlist = {
  items: WishlistItem[];
  count: number;
};

export type CommerceApiResponse<T> = ApiResponse<T>;
