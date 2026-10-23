package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
    @NotNull @Positive Long shippingAddressId,
    @Size(max = 255) String couponCode,
    @Size(max = 255) String freeShippingCouponCode,
    @Pattern(regexp = "(?i)^(cod|vnpay)?$", message = "Phương thức thanh toán không hợp lệ") String paymentMethod,
    @Min(value = 0, message = "Điểm sử dụng không hợp lệ") Integer loyaltyPoints,
    @Pattern(regexp = "(?i)^(internal|ghn)?$", message = "Đơn vị giao hàng không hợp lệ") String shippingProvider,
    @DecimalMin("0") BigDecimal expectedTotal
) {
    public CheckoutRequest(Long shippingAddressId, String couponCode, String freeShippingCouponCode,
            String paymentMethod, Integer loyaltyPoints) {
        this(shippingAddressId, couponCode, freeShippingCouponCode, paymentMethod, loyaltyPoints, null, null);
    }
}
