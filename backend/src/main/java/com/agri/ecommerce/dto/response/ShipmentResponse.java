package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShipmentResponse(String provider, String providerOrderCode, String shippingStatus,
    BigDecimal customerShippingFee, BigDecimal carrierShippingFee, BigDecimal codAmount,
    LocalDateTime estimatedDeliveryAt, LocalDateTime codTransferredAt, LocalDateTime createdAt, String lastError) {}
