package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.CouponRepository;
import com.agri.ecommerce.repository.CouponUsageRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CouponEngineServiceTest {
    @Mock CouponRepository coupons;
    @Mock CouponUsageRepository usages;
    CouponEngineService service;
    CartItem cartItem;
    Product product;
    Category category;

    @BeforeEach void setUp() {
        service = new CouponEngineService(coupons, usages);
        category = new Category(); ReflectionTestUtils.setField(category, "id", 5L);
        product = new Product(); ReflectionTestUtils.setField(product, "id", 10L);
        product.setCategory(category); product.setPrice(new BigDecimal("200000"));
        cartItem = new CartItem(); cartItem.setProduct(product); cartItem.setQuantity(2);
        lenient().when(coupons.save(any(Coupon.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(usages.save(any(CouponUsage.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test void percentageCouponUsesEligibleSubtotalAndMaximumDiscount() {
        Coupon coupon = coupon(1L, "PERCENT", CouponType.ORDER_DISCOUNT);
        coupon.setDiscountType(DiscountType.PERCENTAGE); coupon.setDiscountPercentage(50);
        coupon.setMaxDiscountAmount(new BigDecimal("70000"));
        found(coupon, false);
        var quote = service.preview(7L, List.of(cartItem), bd("400000"), bd("30000"), coupon.getCode(), null);
        assertThat(quote.productDiscount()).isEqualByComparingTo("70000");
    }

    @Test void fixedCouponCannotExceedEligibleSubtotal() {
        Coupon coupon = coupon(2L, "FIXED", CouponType.ORDER_DISCOUNT);
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT); coupon.setDiscountAmount(bd("500000"));
        found(coupon, false);
        assertThat(service.preview(7L, List.of(cartItem), bd("400000"), bd("30000"), coupon.getCode(), null).productDiscount())
            .isEqualByComparingTo("400000");
    }

    @Test void freeShippingCouponRespectsMaximumAmount() {
        Coupon coupon = coupon(3L, "SHIP", CouponType.FREESHIP); coupon.setMaxDiscountAmount(bd("15000"));
        found(coupon, false);
        assertThat(service.preview(7L, List.of(cartItem), bd("400000"), bd("30000"), null, coupon.getCode()).shippingDiscount())
            .isEqualByComparingTo("15000");
    }

    @Test void rejectsMinimumOrderNotMet() {
        Coupon coupon = coupon(4L, "MIN", CouponType.ORDER_DISCOUNT); coupon.setMinimumOrderAmount(bd("500000"));
        found(coupon, false);
        assertCode(() -> service.preview(7L, List.of(cartItem), bd("400000"), bd("30000"), coupon.getCode(), null), "COUPON_MINIMUM_NOT_MET");
    }

    @Test void rejectsInactiveExpiredAndNotStartedCoupons() {
        Coupon inactive = coupon(5L, "OFF", CouponType.ORDER_DISCOUNT); inactive.setActive(false); found(inactive, false);
        assertCode(() -> service.preview(7L, List.of(cartItem), bd("400000"), bd("30000"), inactive.getCode(), null), "COUPON_INACTIVE");
        Coupon expired = coupon(6L, "OLD", CouponType.ORDER_DISCOUNT); expired.setExpiresAt(LocalDateTime.now().minusMinutes(1)); found(expired, false);
        assertCode(() -> service.preview(7L, List.of(cartItem), bd("400000"), bd("30000"), expired.getCode(), null), "COUPON_EXPIRED");
        Coupon future = coupon(7L, "FUTURE", CouponType.ORDER_DISCOUNT); future.setStartsAt(LocalDateTime.now().plusMinutes(5)); found(future, false);
        assertCode(() -> service.preview(7L, List.of(cartItem), bd("400000"), bd("30000"), future.getCode(), null), "COUPON_NOT_STARTED");
    }

    @Test void productAndCategoryScopesOnlyDiscountEligibleItems() {
        Product other = new Product(); ReflectionTestUtils.setField(other, "id", 11L); other.setCategory(category); other.setPrice(bd("100000"));
        CartItem otherItem = new CartItem(); otherItem.setProduct(other); otherItem.setQuantity(1);
        Coupon productCoupon = coupon(8L, "PRODUCT", CouponType.ORDER_DISCOUNT); productCoupon.setScopeType(CouponScopeType.PRODUCT);
        productCoupon.getProducts().add(product); found(productCoupon, false);
        assertThat(service.preview(7L, List.of(cartItem, otherItem), bd("500000"), BigDecimal.ZERO, productCoupon.getCode(), null).productDiscount())
            .isEqualByComparingTo("40000");
        Coupon categoryCoupon = coupon(9L, "CATEGORY", CouponType.ORDER_DISCOUNT); categoryCoupon.setScopeType(CouponScopeType.CATEGORY);
        categoryCoupon.getCategories().add(category); found(categoryCoupon, false);
        assertThat(service.preview(7L, List.of(cartItem, otherItem), bd("500000"), BigDecimal.ZERO, categoryCoupon.getCode(), null).productDiscount())
            .isEqualByComparingTo("50000");
    }

    @Test void rejectsCartOutsideCouponScope() {
        Coupon coupon = coupon(10L, "OTHER", CouponType.ORDER_DISCOUNT); coupon.setScopeType(CouponScopeType.PRODUCT);
        Product other = new Product(); ReflectionTestUtils.setField(other, "id", 99L); coupon.getProducts().add(other); found(coupon, false);
        assertCode(() -> service.preview(7L, List.of(cartItem), bd("400000"), bd("30000"), coupon.getCode(), null), "COUPON_SCOPE_NOT_APPLICABLE");
    }

    @Test void enforcesGlobalAndPerUserUsageLimits() {
        Coupon exhausted = coupon(11L, "FULL", CouponType.ORDER_DISCOUNT); exhausted.setUsageLimit(1); exhausted.setTimesUsed(1); found(exhausted, false);
        assertCode(() -> service.preview(7L, List.of(cartItem), bd("400000"), bd("30000"), exhausted.getCode(), null), "COUPON_USAGE_LIMIT_REACHED");
        Coupon personal = coupon(12L, "PERSONAL", CouponType.ORDER_DISCOUNT); personal.setUsageLimitPerUser(1); found(personal, false);
        when(usages.countByCoupon_IdAndUser_IdAndStatusIn(eq(12L), eq(7L), anyCollection())).thenReturn(1L);
        assertCode(() -> service.preview(7L, List.of(cartItem), bd("400000"), bd("30000"), personal.getCode(), null), "COUPON_USER_LIMIT_REACHED");
    }

    @Test void reservationOccupiesLastSlotBeforeAnotherCheckoutCanValidate() {
        Coupon coupon = coupon(13L, "LAST", CouponType.ORDER_DISCOUNT); coupon.setUsageLimit(1); found(coupon, true);
        var quote = service.quoteForReservation(7L, List.of(cartItem), bd("400000"), bd("30000"), coupon.getCode(), null);
        User user = new User(); ReflectionTestUtils.setField(user, "id", 7L);
        Order order = new Order(); ReflectionTestUtils.setField(order, "id", 20L);
        service.reserve(user, order, quote);
        assertThat(coupon.getTimesUsed()).isEqualTo(1);
        assertCode(() -> service.quoteForReservation(8L, List.of(cartItem), bd("400000"), bd("30000"), coupon.getCode(), null), "COUPON_USAGE_LIMIT_REACHED");
    }

    @Test void releaseAndUseTransitionsAreIdempotentAndUsedCouponIsNotReissued() {
        Coupon coupon = coupon(14L, "STATE", CouponType.ORDER_DISCOUNT); coupon.setTimesUsed(1);
        User user = new User(); Order order = new Order(); ReflectionTestUtils.setField(order, "id", 21L);
        CouponUsage reserved = usage(coupon, user, order, CouponUsageStatus.RESERVED);
        when(usages.findAllByOrderIdForUpdate(21L)).thenReturn(List.of(reserved));
        when(coupons.findByIdForUpdate(14L)).thenReturn(Optional.of(coupon));
        service.releaseForCancellation(order, false); service.releaseForCancellation(order, false);
        assertThat(reserved.getStatus()).isEqualTo(CouponUsageStatus.RELEASED);
        assertThat(coupon.getTimesUsed()).isZero();
        verify(coupons, times(1)).findByIdForUpdate(14L);

        CouponUsage used = usage(coupon, user, order, CouponUsageStatus.USED); coupon.setTimesUsed(1);
        when(usages.findAllByOrderIdForUpdate(21L)).thenReturn(List.of(used));
        service.releaseForCancellation(order, true);
        assertThat(coupon.getTimesUsed()).isEqualTo(1);
        assertThat(used.getStatus()).isEqualTo(CouponUsageStatus.USED);
    }

    @Test void repeatedPaymentConfirmationMarksReservationUsedOnlyOnce() {
        Coupon coupon = coupon(15L, "PAYMENT", CouponType.ORDER_DISCOUNT);
        User user = new User(); Order order = new Order(); ReflectionTestUtils.setField(order, "id", 22L);
        CouponUsage usage = usage(coupon, user, order, CouponUsageStatus.RESERVED);
        when(usages.findAllByOrderIdForUpdate(22L)).thenReturn(List.of(usage));

        service.markUsed(order); service.markUsed(order);

        assertThat(usage.getStatus()).isEqualTo(CouponUsageStatus.USED);
        assertThat(usage.getUsedAt()).isNotNull();
        verify(usages, times(1)).save(usage);
    }

    private Coupon coupon(Long id, String code, CouponType type) {
        Coupon coupon = new Coupon(); ReflectionTestUtils.setField(coupon, "id", id);
        coupon.setCode(code); coupon.setName(code); coupon.setCouponType(type); coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountPercentage(type == CouponType.ORDER_DISCOUNT ? 10 : 0); coupon.setScopeType(CouponScopeType.ALL); coupon.setActive(true);
        return coupon;
    }
    private CouponUsage usage(Coupon coupon, User user, Order order, CouponUsageStatus status) {
        CouponUsage usage = new CouponUsage(); usage.setCoupon(coupon); usage.setUser(user); usage.setOrder(order); usage.setStatus(status); return usage;
    }
    private void found(Coupon coupon, boolean lock) {
        if (lock) when(coupons.findByCodeForUpdate(coupon.getCode())).thenReturn(Optional.of(coupon));
        else when(coupons.findByCodeIgnoreCase(coupon.getCode())).thenReturn(Optional.of(coupon));
    }
    private BigDecimal bd(String value) { return new BigDecimal(value); }
    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).extracting("code").isEqualTo(code);
    }
}
