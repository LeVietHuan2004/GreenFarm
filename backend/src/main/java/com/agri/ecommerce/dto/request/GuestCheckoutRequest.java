package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.*;

public record GuestCheckoutRequest(
    @NotBlank @Size(min=2,max=100) String name,
    @NotBlank @Pattern(regexp="^(?:\\+84|0)[0-9]{9,10}$",message="Số điện thoại không hợp lệ") String phone,
    @NotBlank @Email @Size(max=255) String email,
    @NotBlank @Size(max=255) String shippingAddress,
    @NotBlank @Size(max=100) String shippingCity,
    @Pattern(regexp="(?i)^(standard|express)$",message="Phương thức giao hàng không hợp lệ") String shippingMethod,
    @Size(max=255) String couponCode,
    @Size(max=255) String freeShippingCouponCode,
    @Pattern(regexp="(?i)^(cod|vnpay)?$",message="Phương thức thanh toán không hợp lệ") String paymentMethod,
    @NotBlank @Pattern(regexp="^[A-Za-z0-9_-]{16,64}$",message="Khóa chống trùng không hợp lệ") String idempotencyKey
) {}
