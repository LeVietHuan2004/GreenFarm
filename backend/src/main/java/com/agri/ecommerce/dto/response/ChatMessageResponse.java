package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageResponse(
    Long id,
    String sender,
    String content,
    LocalDateTime createdAt,
    List<ChatProductResponse> products
) {}
