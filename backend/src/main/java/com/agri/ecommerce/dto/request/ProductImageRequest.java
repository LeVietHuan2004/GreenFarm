package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImageRequest(
    @NotBlank(message = "Duong dan anh khong duoc de trong")
    @Size(max = 255, message = "Duong dan anh qua dai")
    String image
) {
}
