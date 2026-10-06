import { apiClient } from "@/lib/axios-client";
import type {
  CatalogApiResponse,
  Category,
  CategoryInput,
  PageData,
  Product,
  ProductFilters,
  ProductImage,
  ProductInput,
  ProductStatus
} from "@/types/catalog";

function compactParams(filters: ProductFilters) {
  return Object.fromEntries(
    Object.entries(filters).filter(([, value]) => value !== undefined && value !== "")
  );
}

export async function getPublicCategories() {
  const response = await apiClient.get<CatalogApiResponse<Category[]>>("/public/categories");
  return response.data.data;
}

export async function getPublicCategory(slug: string) {
  const response = await apiClient.get<CatalogApiResponse<Category>>(
    `/public/categories/${encodeURIComponent(slug)}`
  );
  return response.data.data;
}

export async function getPublicProducts(filters: ProductFilters = {}) {
  const response = await apiClient.get<CatalogApiResponse<PageData<Product>>>(
    "/public/products",
    { params: compactParams(filters) }
  );
  return response.data.data;
}

export async function getPublicProduct(slug: string) {
  const response = await apiClient.get<CatalogApiResponse<Product>>(
    `/public/products/${encodeURIComponent(slug)}`
  );
  return response.data.data;
}

export async function getAdminCategories() {
  const response = await apiClient.get<CatalogApiResponse<Category[]>>("/admin/categories");
  return response.data.data;
}

export async function createCategory(input: CategoryInput) {
  const response = await apiClient.post<CatalogApiResponse<Category>>("/admin/categories", input);
  return response.data.data;
}

export async function updateCategory(categoryId: number, input: CategoryInput) {
  const response = await apiClient.put<CatalogApiResponse<Category>>(
    `/admin/categories/${categoryId}`,
    input
  );
  return response.data.data;
}

export async function deleteCategory(categoryId: number) {
  await apiClient.delete(`/admin/categories/${categoryId}`);
}

export async function getAdminProducts(filters: ProductFilters = {}) {
  const response = await apiClient.get<CatalogApiResponse<PageData<Product>>>(
    "/admin/products",
    { params: compactParams(filters) }
  );
  return response.data.data;
}

export async function createProduct(input: ProductInput) {
  const response = await apiClient.post<CatalogApiResponse<Product>>("/admin/products", input);
  return response.data.data;
}

export async function updateProduct(productId: number, input: ProductInput) {
  const response = await apiClient.put<CatalogApiResponse<Product>>(
    `/admin/products/${productId}`,
    input
  );
  return response.data.data;
}

export async function updateProductStatus(productId: number, status: ProductStatus) {
  const response = await apiClient.patch<CatalogApiResponse<Product>>(
    `/admin/products/${productId}/status`,
    { status }
  );
  return response.data.data;
}

export async function addProductImage(productId: number, image: string) {
  const response = await apiClient.post<CatalogApiResponse<ProductImage>>(
    `/admin/products/${productId}/images`,
    { image }
  );
  return response.data.data;
}

export async function uploadProductImage(productId: number, file: File) {
  const body = new FormData();
  body.append("file", file);
  const response = await apiClient.post<CatalogApiResponse<ProductImage>>(
    `/admin/products/${productId}/images/upload`, body, { headers: { "Content-Type": "multipart/form-data" } }
  );
  return response.data.data;
}

export async function uploadCatalogImage(file: File) {
  const body = new FormData();
  body.append("file", file);
  const response = await apiClient.post<CatalogApiResponse<{ path:string }>>(
    "/admin/uploads/images", body, { headers: { "Content-Type": "multipart/form-data" } }
  );
  return response.data.data.path;
}

export async function deleteProductImage(productId: number, imageId: number) {
  await apiClient.delete(`/admin/products/${productId}/images/${imageId}`);
}
