package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record ReviewResponse(Long id, Long productId, Long userId, String userName, int rating,
                             String comment, LocalDateTime createdAt, LocalDateTime updatedAt) {}
