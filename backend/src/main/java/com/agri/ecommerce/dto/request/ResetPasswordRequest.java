package com.agri.ecommerce.dto.request;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(@NotBlank @Size(max = 128) String token, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
