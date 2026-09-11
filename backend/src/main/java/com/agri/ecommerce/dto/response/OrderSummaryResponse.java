package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(Long id, String status, int itemCount, BigDecimal total, String recipientName,
                                   String shippingCity, LocalDateTime createdAt, Long deliveryStaffId,
                                   String deliveryStaffName) {}
