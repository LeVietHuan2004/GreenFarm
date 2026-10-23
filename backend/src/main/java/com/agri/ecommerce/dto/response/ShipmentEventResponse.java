package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record ShipmentEventResponse(Long id, String eventType, String shippingStatus,
    LocalDateTime eventAt, LocalDateTime receivedAt, String description) {}
