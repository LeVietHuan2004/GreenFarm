package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record InventoryTransactionResponse(
    Long transactionId,
    Long productId,
    String productName,
    Long batchId,
    String batchCode,
    String type,
    int quantity,
    int quantityBefore,
    int quantityAfter,
    int reservedBefore,
    int reservedAfter,
    Long referenceId,
    String reason,
    Long createdBy,
    String createdByName,
    LocalDateTime createdAt
) {}
