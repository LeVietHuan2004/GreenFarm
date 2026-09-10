package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductStatusRequest(
    @NotBlank(message = "Trang thai khong duoc de trong")
    @Size(max = 30, message = "Trang thai qua dai")
    String status
) {
}
