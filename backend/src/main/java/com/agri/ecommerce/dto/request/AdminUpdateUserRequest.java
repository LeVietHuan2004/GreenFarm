package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequest(
    @Size(max = 50, message = "Ten vai tro qua dai")
    String role,

    @Size(max = 20, message = "Trang thai qua dai")
    String status
) {
}
