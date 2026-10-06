package com.agri.ecommerce.dto.response;
import java.time.LocalDateTime;
public record GuestSessionResponse(String token, LocalDateTime expiresAt) {}
