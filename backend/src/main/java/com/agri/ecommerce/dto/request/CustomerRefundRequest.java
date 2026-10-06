package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRefundRequest(
    @NotBlank @Size(max = 100) String reason,
    @Size(max = 1000) String details
) {}
