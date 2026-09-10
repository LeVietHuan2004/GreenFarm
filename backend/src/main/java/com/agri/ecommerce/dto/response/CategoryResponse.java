package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record CategoryResponse(
    Long id,
    String name,
    String nameEn,
    String slug,
    String description,
    String descriptionEn,
    String image,
    long productCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
