package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String type, String message, String link, boolean read,
                                   LocalDateTime createdAt) {}
