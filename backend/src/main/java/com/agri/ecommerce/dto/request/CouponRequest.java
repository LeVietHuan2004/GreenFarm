package com.agri.ecommerce.dto.request;

import com.agri.ecommerce.entity.CouponType;
import com.agri.ecommerce.entity.CouponScopeType;
import com.agri.ecommerce.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record CouponRequest(
    @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 500) String description,
    @NotNull CouponType couponType,
    @NotNull DiscountType discountType,
    @Min(0) @Max(100) Integer discountPercentage,
    @DecimalMin(value = "0.00", inclusive = true) BigDecimal discountAmount,
    @DecimalMin(value = "0.00", inclusive = false) BigDecimal maxDiscountAmount,
    @DecimalMin(value = "0.00", inclusive = true) BigDecimal minimumOrderAmount,
    @NotNull CouponScopeType scopeType,
    Set<@NotNull @Positive Long> categoryIds,
    Set<@NotNull @Positive Long> productIds,
    LocalDateTime startsAt,
    LocalDateTime expiresAt,
    @Min(1) Integer usageLimit,
    @Min(1) Integer usageLimitPerUser,
    @NotNull Boolean active
) {}
