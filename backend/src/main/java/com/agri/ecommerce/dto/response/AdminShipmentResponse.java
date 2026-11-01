package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminShipmentResponse(Long orderId, String orderStatus, String recipientName,
    String recipientPhone, String shippingAddress, String shippingCity, String providerOrderCode,
    String shippingStatus, BigDecimal carrierShippingFee, BigDecimal customerShippingFee,
    BigDecimal codAmount, LocalDateTime createdAt) {}
