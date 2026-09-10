package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Pattern(regexp = "^(?:\\+84|0)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng") String phone,
    @NotBlank @Size(max = 255) String address,
    @NotBlank @Size(max = 100) String city,
    boolean defaultAddress
) {}
