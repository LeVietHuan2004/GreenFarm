package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WishlistItemRequest(
    @NotNull(message = "Ma san pham la bat buoc")
    @Positive(message = "Ma san pham khong hop le")
    Long productId
) {
}
