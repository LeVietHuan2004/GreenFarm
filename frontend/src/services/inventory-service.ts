import { apiClient } from "@/lib/axios-client";
import type { ApiResponse } from "@/types/auth";
import type { InventoryAdjustment, InventoryBatch, InventoryDetail, InventoryImport, InventoryPage, InventoryProduct, InventorySummary, InventoryTransaction, Supplier, SupplierInput } from "@/types/inventory";

type Filters = Record<string, string | number | undefined>;
const compact = (filters: Filters) => Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== undefined && value !== ""));
const data = async <T>(request: Promise<{ data: ApiResponse<T> }>) => (await request).data.data;

export const inventoryService = {
  summary: () => data(apiClient.get<ApiResponse<InventorySummary>>("/admin/inventory/summary")),
  inventory: (filters: Filters) => data(apiClient.get<ApiResponse<InventoryPage<InventoryProduct>>>("/admin/inventory", { params: compact(filters) })),
  detail: (productId: number) => data(apiClient.get<ApiResponse<InventoryDetail>>(`/admin/inventory/${productId}`)),
  batches: (filters: Filters) => data(apiClient.get<ApiResponse<InventoryPage<InventoryBatch>>>("/admin/inventory/batches", { params: compact(filters) })),
  transactions: (filters: Filters) => data(apiClient.get<ApiResponse<InventoryPage<InventoryTransaction>>>("/admin/inventory/transactions", { params: compact(filters) })),
  importBatch: (input: InventoryImport) => data(apiClient.post<ApiResponse<InventoryBatch>>("/admin/inventory/import", input)),
  adjust: (input: InventoryAdjustment) => data(apiClient.post<ApiResponse<InventoryBatch>>("/admin/inventory/adjust", input)),
  suppliers: (filters: Filters) => data(apiClient.get<ApiResponse<InventoryPage<Supplier>>>("/admin/suppliers", { params: compact(filters) })),
  createSupplier: (input: SupplierInput) => data(apiClient.post<ApiResponse<Supplier>>("/admin/suppliers", input)),
  updateSupplier: (id: number, input: SupplierInput) => data(apiClient.put<ApiResponse<Supplier>>(`/admin/suppliers/${id}`, input)),
  deactivateSupplier: (id: number) => data(apiClient.delete<ApiResponse<Supplier>>(`/admin/suppliers/${id}`))
};
