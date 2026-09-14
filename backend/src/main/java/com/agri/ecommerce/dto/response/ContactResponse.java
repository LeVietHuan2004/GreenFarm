package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record ContactResponse(Long id, Long userId, String fullName, String phoneNumber, String email,
                              String message, String status, String response, Long respondedById,
                              String respondedByName, LocalDateTime respondedAt, LocalDateTime createdAt,
                              LocalDateTime updatedAt) {}
