package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AvailableCouponResponse(
    String code,
    String name,
    String description,
    String couponType,
    String discountType,
    int discountPercentage,
    BigDecimal discountAmount,
    BigDecimal maxDiscountAmount,
    BigDecimal minimumOrderAmount,
    String scopeType,
    List<Long> categoryIds,
    List<Long> productIds,
    LocalDateTime expiresAt
) {}
