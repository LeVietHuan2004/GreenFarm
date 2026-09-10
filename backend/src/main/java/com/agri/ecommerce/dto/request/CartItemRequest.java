package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(
    @NotNull(message = "Ma san pham la bat buoc")
    @Positive(message = "Ma san pham khong hop le")
    Long productId,

    @NotNull(message = "So luong la bat buoc")
    @Positive(message = "So luong phai lon hon 0")
    Integer quantity
) {
}
