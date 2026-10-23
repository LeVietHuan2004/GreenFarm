package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
    Long id,
    String name,
    String nameEn,
    String slug,
    CategorySummaryResponse category,
    String description,
    String descriptionEn,
    BigDecimal price,
    int stock,
    String status,
    String unit,
    String unitEn,
    boolean ghnEligible,
    Integer packageWeightG,
    Integer packageLengthCm,
    Integer packageWidthCm,
    Integer packageHeightCm,
    List<ProductImageResponse> images,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
