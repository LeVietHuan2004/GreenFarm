package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Mat khau hien tai khong duoc de trong")
    String currentPassword,

    @NotBlank(message = "Mat khau moi khong duoc de trong")
    @Size(min = 8, max = 72, message = "Mat khau moi phai co tu 8 den 72 ky tu")
    String newPassword
) {
}
