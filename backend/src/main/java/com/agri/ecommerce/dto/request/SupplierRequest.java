package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
    @NotBlank @Size(max = 40) String supplierCode,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 30) String phone,
    @Email @Size(max = 255) String email,
    @Size(max = 500) String address,
    @NotBlank String status,
    @Size(max = 1000) String note
) {}
