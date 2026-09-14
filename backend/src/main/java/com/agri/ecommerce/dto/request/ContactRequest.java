package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.*;

public record ContactRequest(
    @NotBlank @Size(max = 255) String fullName,
    @Size(max = 30) String phoneNumber,
    @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 10, max = 2000) String message
) {}
