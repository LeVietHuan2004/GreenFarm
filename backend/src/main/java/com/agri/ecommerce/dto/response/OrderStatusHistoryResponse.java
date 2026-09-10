package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record OrderStatusHistoryResponse(Long id, String status, String note, LocalDateTime changedAt) {}
