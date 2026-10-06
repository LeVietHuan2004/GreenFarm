package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;

/** A small, public-only product card attached to a chat reply. */
public record ChatProductResponse(
    Long id,
    String name,
    String slug,
    BigDecimal price,
    String unit,
    int stock,
    String image
) {
}
