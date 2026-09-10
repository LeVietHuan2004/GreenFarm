package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;

public record CheckoutPreviewResponse(BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discountAmount,
                                      BigDecimal total, String couponCode, String discountDescription) {}
