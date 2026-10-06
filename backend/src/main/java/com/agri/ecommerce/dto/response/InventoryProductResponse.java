package com.agri.ecommerce.dto.response;

public record InventoryProductResponse(
    Long productId,
    String productCode,
    String productName,
    String categoryName,
    String unit,
    long totalQuantity,
    long availableQuantity,
    String stockStatus,
    boolean hasExpiringBatch,
    boolean hasExpiredBatch,
    long batchCount
) {}
