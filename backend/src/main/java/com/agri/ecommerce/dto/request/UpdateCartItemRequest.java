package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateCartItemRequest(
    @NotNull(message = "So luong la bat buoc")
    @Positive(message = "So luong phai lon hon 0")
    Integer quantity
) {
}
