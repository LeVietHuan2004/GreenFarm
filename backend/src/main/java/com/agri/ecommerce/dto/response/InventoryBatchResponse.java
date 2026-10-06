package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InventoryBatchResponse(
    Long id,
    String batchCode,
    Long productId,
    String productName,
    int quantity,
    int remainingQuantity,
    int reservedQuantity,
    int availableQuantity,
    String unit,
    BigDecimal importPrice,
    LocalDate manufactureDate,
    LocalDate expiryDate,
    Long supplierId,
    String supplierName,
    LocalDateTime importedAt,
    String note,
    Long createdBy,
    String expiryStatus
) {}
