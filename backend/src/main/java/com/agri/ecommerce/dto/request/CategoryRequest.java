package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank(message = "Ten danh muc khong duoc de trong")
    @Size(max = 255, message = "Ten danh muc qua dai")
    String name,

    @Size(max = 255, message = "Ten tieng Anh qua dai")
    String nameEn,

    @Size(max = 255, message = "Slug qua dai")
    String slug,

    @Size(max = 5000, message = "Mo ta qua dai")
    String description,

    @Size(max = 5000, message = "Mo ta tieng Anh qua dai")
    String descriptionEn,

    @Size(max = 1024, message = "Duong dan anh qua dai")
    String image
) {
}
