package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
    @NotNull @Positive Long shippingAddressId,
    @Size(max = 255) String couponCode,
    @Pattern(regexp = "(?i)^(cod|vnpay)?$", message = "Phương thức thanh toán không hợp lệ") String paymentMethod
) {}
