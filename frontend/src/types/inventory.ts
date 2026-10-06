import type { PageData } from "@/types/catalog";

export type InventorySummary = { productsInStock: number; lowStockProducts: number; expiringBatches: number; expiredBatches: number };
export type InventoryProduct = { productId: number; productCode: string; productName: string; categoryName: string; unit: string | null; totalQuantity: number; availableQuantity: number; stockStatus: "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK"; hasExpiringBatch: boolean; hasExpiredBatch: boolean; batchCount: number };
export type InventoryBatch = { id: number; batchCode: string; productId: number; productName: string; quantity: number; remainingQuantity: number; reservedQuantity: number; availableQuantity: number; unit: string | null; importPrice: number; manufactureDate: string | null; expiryDate: string | null; supplierId: number | null; supplierName: string | null; importedAt: string; note: string | null; createdBy: number | null; expiryStatus: "NO_EXPIRY" | "VALID" | "EXPIRING" | "EXPIRED" };
export type InventoryTransaction = { transactionId: number; productId: number; productName: string; batchId: number; batchCode: string; type: string; quantity: number; quantityBefore: number; quantityAfter: number; reservedBefore: number; reservedAfter: number; referenceId: number | null; reason: string | null; createdBy: number | null; createdByName: string | null; createdAt: string };
export type Supplier = { id: number; supplierCode: string; name: string; phone: string | null; email: string | null; address: string | null; status: "active" | "inactive"; note: string | null; createdAt: string; updatedAt: string };
export type InventoryDetail = { product: InventoryProduct; batches: InventoryBatch[] };
export type InventoryPage<T> = PageData<T>;
export type InventoryImport = { productId: number; quantity: number; importPrice: number; manufactureDate: string | null; expiryDate: string; supplierId: number | null; note: string | null };
export type InventoryAdjustment = { batchId: number; quantityChange: number; type: string; reason: string };
export type SupplierInput = { supplierCode: string; name: string; phone: string | null; email: string | null; address: string | null; status: "active" | "inactive"; note: string | null };
