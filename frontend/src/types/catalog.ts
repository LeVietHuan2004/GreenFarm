import type { ApiResponse } from "@/types/auth";

export type ProductStatus = "in_stock" | "out_of_stock" | "hidden";

export type Category = {
  id: number;
  name: string;
  nameEn: string | null;
  slug: string;
  description: string | null;
  descriptionEn: string | null;
  image: string | null;
  productCount: number;
  createdAt: string;
  updatedAt: string;
};

export type CategorySummary = Pick<Category, "id" | "name" | "slug">;

export type ProductImage = {
  id: number;
  image: string;
};

export type Product = {
  id: number;
  name: string;
  nameEn: string | null;
  slug: string;
  category: CategorySummary;
  description: string | null;
  descriptionEn: string | null;
  price: number;
  stock: number;
  status: ProductStatus;
  unit: string | null;
  unitEn: string | null;
  images: ProductImage[];
  createdAt: string;
  updatedAt: string;
};

export type PageData<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type ProductFilters = {
  search?: string;
  category?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  status?: ProductStatus;
  page?: number;
  size?: number;
  sort?: string;
};

export type CategoryInput = {
  name: string;
  nameEn?: string;
  slug?: string;
  description?: string;
  descriptionEn?: string;
  image?: string;
};

export type ProductInput = {
  name: string;
  nameEn?: string;
  slug?: string;
  categoryId: number;
  description?: string;
  descriptionEn?: string;
  price: number;
  stock: number;
  status?: ProductStatus;
  unit?: string;
  unitEn?: string;
};

export type CatalogApiResponse<T> = ApiResponse<T>;
