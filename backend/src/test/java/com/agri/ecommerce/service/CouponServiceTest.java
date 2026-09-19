package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.dto.request.CouponRequest;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {
    @Mock CouponRepository coupons;
    @Mock CouponUsageRepository usages;
    @Mock CategoryRepository categories;
    @Mock ProductRepository products;
    @Mock CouponEngineService couponEngine;
    CouponService service;

    @BeforeEach void setUp() {
        service = new CouponService(coupons, usages, categories, products, couponEngine);
        lenient().when(coupons.save(any(Coupon.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test void percentageCouponIsNormalizedAndCreated() {
        CouponRequest request = request("save20", CouponType.ORDER_DISCOUNT, DiscountType.PERCENTAGE, 20, null, null, null);
        var response = service.create(request);
        assertThat(response.code()).isEqualTo("SAVE20");
        assertThat(response.discountPercentage()).isEqualTo(20);
        assertThat(response.discountAmount()).isNull();
        verify(coupons).save(any(Coupon.class));
    }

    @Test void fixedCouponRequiresPositiveAmount() {
        CouponRequest request = request("FIXED", CouponType.ORDER_DISCOUNT, DiscountType.FIXED_AMOUNT, 0, BigDecimal.ZERO, null, null);
        assertThatThrownBy(() -> service.create(request)).extracting("code").isEqualTo("INVALID_COUPON_VALUE");
    }

    @Test void freeshipCouponIgnoresDiscountValues() {
        CouponRequest request = request("SHIPFREE", CouponType.FREESHIP, DiscountType.FIXED_AMOUNT, 88, new BigDecimal("50000"), null, null);
        var response = service.create(request);
        assertThat(response.discountPercentage()).isZero();
        assertThat(response.discountAmount()).isNull();
        assertThat(response.discountType()).isEqualTo("PERCENTAGE");
    }

    @Test void startMustPrecedeExpiry() {
        LocalDateTime now = LocalDateTime.now();
        CouponRequest request = request("BADDATE", CouponType.ORDER_DISCOUNT, DiscountType.PERCENTAGE, 10, null, now, now.minusMinutes(1));
        assertThatThrownBy(() -> service.create(request)).extracting("code").isEqualTo("INVALID_COUPON_PERIOD");
    }

    @Test void protectedDiscountAndScopeCannotChangeAfterAnyUsage() {
        Coupon coupon = existingFixedCoupon(new BigDecimal("10000.00"));
        when(coupons.findById(7L)).thenReturn(java.util.Optional.of(coupon));
        when(usages.existsByCoupon_Id(7L)).thenReturn(true);

        CouponRequest changed = request("FIXED", CouponType.ORDER_DISCOUNT, DiscountType.FIXED_AMOUNT,
            0, new BigDecimal("12000"), null, null);

        assertThatThrownBy(() -> service.update(7L, changed))
            .extracting("code").isEqualTo("COUPON_IMMUTABLE_AFTER_USE");
        verify(coupons, never()).save(coupon);
    }

    @Test void equivalentDecimalScaleDoesNotCountAsProtectedChange() {
        Coupon coupon = existingFixedCoupon(new BigDecimal("10000.00"));
        when(coupons.findById(7L)).thenReturn(java.util.Optional.of(coupon));
        when(usages.existsByCoupon_Id(7L)).thenReturn(true);

        CouponRequest equivalent = request("FIXED", CouponType.ORDER_DISCOUNT, DiscountType.FIXED_AMOUNT,
            0, new BigDecimal("10000"), null, null);

        service.update(7L, equivalent);
        verify(coupons).save(coupon);
    }

    @Test void couponWithUsageHistoryCannotBeDeleted() {
        Coupon coupon = existingFixedCoupon(new BigDecimal("10000"));
        when(coupons.findById(7L)).thenReturn(java.util.Optional.of(coupon));
        when(usages.existsByCoupon_Id(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(7L)).extracting("code").isEqualTo("COUPON_IN_USE");
        verify(coupons, never()).delete(any(Coupon.class));
    }

    private Coupon existingFixedCoupon(BigDecimal amount) {
        Coupon coupon = new Coupon();
        coupon.setCode("FIXED");
        coupon.setName("FIXED");
        coupon.setCouponType(CouponType.ORDER_DISCOUNT);
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountPercentage(0);
        coupon.setDiscountAmount(amount);
        coupon.setMinimumOrderAmount(BigDecimal.ZERO);
        coupon.setScopeType(CouponScopeType.ALL);
        coupon.setUsageLimit(100);
        coupon.setUsageLimitPerUser(1);
        coupon.setActive(true);
        return coupon;
    }

    private CouponRequest request(String code, CouponType couponType, DiscountType discountType, Integer percentage,
                                  BigDecimal amount, LocalDateTime startsAt, LocalDateTime expiresAt) {
        return new CouponRequest(code, code, null, couponType, discountType, percentage, amount, null,
            BigDecimal.ZERO, CouponScopeType.ALL, Set.of(), Set.of(), startsAt, expiresAt, 100, 1, true);
    }
}
