package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record WishlistItemResponse(
    Long id,
    ProductResponse product,
    LocalDateTime createdAt
) {
}
