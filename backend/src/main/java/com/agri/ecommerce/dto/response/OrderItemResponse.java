package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(Long id, Long productId, String productSlug, String productName, String productUnit,
                                String productImage, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
