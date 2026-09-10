package com.agri.ecommerce.dto.response;

import java.util.List;

public record WishlistResponse(
    List<WishlistItemResponse> items,
    long count
) {
}
