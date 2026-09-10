package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
    Long id,
    String name,
    String email,
    String status,
    String phoneNumber,
    String avatar,
    String address,
    String role,
    Set<String> permissions,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt
) {
}
