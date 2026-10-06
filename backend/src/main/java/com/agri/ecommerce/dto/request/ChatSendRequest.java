package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatSendRequest(
    @NotBlank @Size(max = 1500) String message,
    @Pattern(regexp = "[A-Za-z0-9_-]{40,128}", message = "Guest token không hợp lệ") String guestToken
) {}
