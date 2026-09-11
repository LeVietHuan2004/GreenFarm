package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.dto.request.CouponRequest;
import com.agri.ecommerce.entity.Coupon;
import com.agri.ecommerce.entity.CouponType;
import com.agri.ecommerce.entity.DiscountType;
import com.agri.ecommerce.repository.CouponRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {
    @Mock CouponRepository coupons;
    CouponService service;

    @BeforeEach void setUp() {
        service = new CouponService(coupons);
        lenient().when(coupons.save(any(Coupon.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test void percentageCouponIsNormalizedAndCreated() {
        CouponRequest request = new CouponRequest("save20", CouponType.ORDER_DISCOUNT, DiscountType.PERCENTAGE,
            20, null, null, null, 100, true);
        var response = service.create(request);
        assertThat(response.code()).isEqualTo("SAVE20");
        assertThat(response.discountPercentage()).isEqualTo(20);
        assertThat(response.discountAmount()).isNull();
        verify(coupons).save(any(Coupon.class));
    }

    @Test void fixedCouponRequiresPositiveAmount() {
        CouponRequest request = new CouponRequest("FIXED", CouponType.ORDER_DISCOUNT, DiscountType.FIXED_AMOUNT,
            0, BigDecimal.ZERO, null, null, null, true);
        assertThatThrownBy(() -> service.create(request)).extracting("code").isEqualTo("INVALID_COUPON_VALUE");
    }

    @Test void freeshipCouponIgnoresDiscountValues() {
        CouponRequest request = new CouponRequest("SHIPFREE", CouponType.FREESHIP, DiscountType.FIXED_AMOUNT,
            88, new BigDecimal("50000"), null, null, null, true);
        var response = service.create(request);
        assertThat(response.discountPercentage()).isZero();
        assertThat(response.discountAmount()).isNull();
        assertThat(response.discountType()).isEqualTo("PERCENTAGE");
    }

    @Test void startMustPrecedeExpiry() {
        LocalDateTime now = LocalDateTime.now();
        CouponRequest request = new CouponRequest("BADDATE", CouponType.ORDER_DISCOUNT, DiscountType.PERCENTAGE,
            10, null, now, now.minusMinutes(1), null, true);
        assertThatThrownBy(() -> service.create(request)).extracting("code").isEqualTo("INVALID_COUPON_PERIOD");
    }
}
