package com.agri.ecommerce.dto.response;

public record InventorySummaryResponse(
    long productsInStock,
    long lowStockProducts,
    long expiringBatches,
    long expiredBatches
) {}
