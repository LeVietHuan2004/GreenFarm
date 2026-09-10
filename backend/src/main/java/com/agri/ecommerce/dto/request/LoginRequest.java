package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong dung dinh dang")
    String email,

    @NotBlank(message = "Mat khau khong duoc de trong")
    String password,

    @Pattern(
        regexp = "customer|staff|delivery_staff|admin",
        message = "Vai tro dang nhap khong hop le"
    )
    String role
) {
}
