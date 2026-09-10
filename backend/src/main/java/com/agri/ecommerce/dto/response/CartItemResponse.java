package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemResponse(
    Long id,
    int quantity,
    BigDecimal lineTotal,
    ProductResponse product,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
