package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryAdjustmentRequest(
    @NotNull Long batchId,
    int quantityChange,
    @NotBlank String type,
    @NotBlank @Size(max = 1000) String reason
) {}
