package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record RefundRequestResponse(Long id, Long orderId, String orderStatus, String customerName,
                                    String reason, String details, String status, String adminNote,
                                    String reviewedBy, LocalDateTime reviewedAt, LocalDateTime createdAt) {}
