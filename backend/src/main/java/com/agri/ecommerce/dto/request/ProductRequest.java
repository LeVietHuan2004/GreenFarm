package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "Ten san pham khong duoc de trong")
    @Size(max = 255, message = "Ten san pham qua dai")
    String name,

    @Size(max = 255, message = "Ten tieng Anh qua dai")
    String nameEn,

    @Size(max = 255, message = "Slug qua dai")
    String slug,

    @NotNull(message = "Danh muc khong duoc de trong")
    @Positive(message = "Danh muc khong hop le")
    Long categoryId,

    @Size(max = 5000, message = "Mo ta qua dai")
    String description,

    @Size(max = 5000, message = "Mo ta tieng Anh qua dai")
    String descriptionEn,

    @NotNull(message = "Gia san pham khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = true, message = "Gia san pham khong duoc am")
    @Digits(integer = 8, fraction = 2, message = "Gia san pham khong hop le")
    BigDecimal price,

    @Min(value = 0, message = "Ton kho khong duoc am")
    int stock,

    @Size(max = 30, message = "Trang thai qua dai")
    String status,

    @Size(max = 255, message = "Don vi qua dai")
    String unit,

    @Size(max = 255, message = "Don vi tieng Anh qua dai")
    String unitEn
) {
}
