package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(min = 2, max = 100, message = "Ho ten phai co tu 2 den 100 ky tu")
    String name,

    @Pattern(
        regexp = "^(?:\\+84|0)[0-9]{9,10}$",
        message = "So dien thoai khong dung dinh dang"
    )
    String phoneNumber,

    @Size(max = 500, message = "Dia chi khong duoc vuot qua 500 ky tu")
    String address,

    @Size(max = 1024, message = "Duong dan anh dai dien qua dai")
    String avatar
) {
}
