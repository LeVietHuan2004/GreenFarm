package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;

public record CouponStatusRequest(@NotNull Boolean active) {}
