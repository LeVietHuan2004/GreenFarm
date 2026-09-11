package com.agri.ecommerce.dto.request;

import com.agri.ecommerce.entity.CouponType;
import com.agri.ecommerce.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponRequest(
    @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
    @NotNull CouponType couponType,
    @NotNull DiscountType discountType,
    @Min(0) @Max(100) Integer discountPercentage,
    @DecimalMin(value = "0.00", inclusive = true) BigDecimal discountAmount,
    LocalDateTime startsAt,
    LocalDateTime expiresAt,
    @Min(1) Integer usageLimit,
    @NotNull Boolean active
) {}
