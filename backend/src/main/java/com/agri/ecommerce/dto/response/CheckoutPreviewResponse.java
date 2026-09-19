package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;

public record CheckoutPreviewResponse(BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discountAmount,
                                      BigDecimal loyaltyDiscountAmount, int loyaltyPointsApplied,
                                      BigDecimal shippingDiscountAmount, BigDecimal total, String couponCode,
                                      String freeShippingCouponCode, String discountDescription,
                                      String shippingDiscountDescription) {}
