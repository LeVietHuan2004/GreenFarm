package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.Size;

public record RefundOrderRequest(
    @Size(max = 500) String note
) {}
