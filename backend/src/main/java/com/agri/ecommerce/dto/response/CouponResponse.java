package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
    Long id,
    String code,
    String couponType,
    String discountType,
    int discountPercentage,
    BigDecimal discountAmount,
    LocalDateTime startsAt,
    LocalDateTime expiresAt,
    Integer usageLimit,
    int timesUsed,
    boolean active,
    boolean currentlyUsable,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
