package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.Size;

public record RefundRequestDecision(@Size(max = 1000) String note) {}
