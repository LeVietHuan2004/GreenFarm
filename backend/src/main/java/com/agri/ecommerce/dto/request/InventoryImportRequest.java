package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InventoryImportRequest(
    @NotNull @Positive Long productId,
    @Positive int quantity,
    @NotNull @DecimalMin("0.00") BigDecimal importPrice,
    LocalDate manufactureDate,
    @NotNull LocalDate expiryDate,
    Long supplierId,
    @Size(max = 1000) String note
) {}
