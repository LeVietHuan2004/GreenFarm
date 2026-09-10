package com.agri.ecommerce.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MergeCartRequest(
    @NotNull(message = "Danh sach san pham la bat buoc")
    @Size(max = 100, message = "Moi lan chi duoc dong bo toi da 100 san pham")
    List<@NotNull @Valid CartItemRequest> items
) {
}
