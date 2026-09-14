package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.*;

public record ReviewRequest(
    @NotNull Long productId,
    @Min(1) @Max(5) int rating,
    @Size(max = 1000) String comment
) {}
