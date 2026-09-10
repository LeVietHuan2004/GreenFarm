package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Ho ten khong duoc de trong")
    @Size(min = 2, max = 100, message = "Ho ten phai co tu 2 den 100 ky tu")
    String name,

    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong dung dinh dang")
    @Size(max = 255, message = "Email qua dai")
    String email,

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 8, max = 72, message = "Mat khau phai co tu 8 den 72 ky tu")
    String password,

    @Pattern(
        regexp = "^(?:\\+84|0)[0-9]{9,10}$",
        message = "So dien thoai khong dung dinh dang"
    )
    String phoneNumber
) {
}
