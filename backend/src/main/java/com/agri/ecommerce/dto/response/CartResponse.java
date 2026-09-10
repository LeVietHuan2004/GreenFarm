package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
    List<CartItemResponse> items,
    long totalItems,
    BigDecimal subtotal
) {
}
