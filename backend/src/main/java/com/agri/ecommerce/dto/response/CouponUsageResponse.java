package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponUsageResponse(Long id, Long userId, String userName, Long orderId,
                                  BigDecimal discountAmount, String status, LocalDateTime createdAt,
                                  LocalDateTime usedAt, LocalDateTime releasedAt) {}
