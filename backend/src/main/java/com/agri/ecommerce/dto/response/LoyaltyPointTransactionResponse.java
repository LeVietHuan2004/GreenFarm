package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record LoyaltyPointTransactionResponse(Long id, String type, int points, String description, LocalDateTime createdAt) {}
