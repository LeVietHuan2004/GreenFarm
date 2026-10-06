package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.response.CouponUsageResponse;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.CouponRepository;
import com.agri.ecommerce.repository.CouponUsageRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CouponEngineService {
    private static final List<CouponUsageStatus> OCCUPYING_STATUSES = List.of(CouponUsageStatus.RESERVED, CouponUsageStatus.USED);
    private final CouponRepository coupons;
    private final CouponUsageRepository usages;

    public CouponEngineService(CouponRepository coupons, CouponUsageRepository usages) {
        this.coupons = coupons;
        this.usages = usages;
    }

    @Transactional(readOnly = true)
    public Quote preview(Long userId, List<CartItem> cart, BigDecimal subtotal, BigDecimal shippingFee,
                         String couponCode, String freeShippingCouponCode) {
        return quote(userId, null, cart, subtotal, shippingFee, couponCode, freeShippingCouponCode, false);
    }

    @Transactional
    public Quote quoteForReservation(Long userId, List<CartItem> cart, BigDecimal subtotal, BigDecimal shippingFee,
                                     String couponCode, String freeShippingCouponCode) {
        return quote(userId, null, cart, subtotal, shippingFee, couponCode, freeShippingCouponCode, true);
    }

    @Transactional(readOnly = true)
    public Quote previewGuest(Long guestSessionId, List<CartItem> cart, BigDecimal subtotal, BigDecimal shippingFee,
                              String couponCode, String freeShippingCouponCode) {
        return quote(null, guestSessionId, cart, subtotal, shippingFee, couponCode, freeShippingCouponCode, false);
    }

    @Transactional
    public Quote quoteGuestForReservation(Long guestSessionId, List<CartItem> cart, BigDecimal subtotal, BigDecimal shippingFee,
                                           String couponCode, String freeShippingCouponCode) {
        return quote(null, guestSessionId, cart, subtotal, shippingFee, couponCode, freeShippingCouponCode, true);
    }

    @Transactional
    public void reserve(User user, Order order, Quote quote) {
        reserveOne(user, null, order, quote.productCoupon(), quote.productDiscount());
        reserveOne(user, null, order, quote.shippingCoupon(), quote.shippingDiscount());
    }

    @Transactional
    public void reserveGuest(GuestSession guestSession, Order order, Quote quote) {
        reserveOne(null, guestSession, order, quote.productCoupon(), quote.productDiscount());
        reserveOne(null, guestSession, order, quote.shippingCoupon(), quote.shippingDiscount());
    }

    @Transactional
    public void markUsed(Order order) {
        LocalDateTime now = LocalDateTime.now();
        for (CouponUsage usage : usages.findAllByOrderIdForUpdate(order.getId())) {
            if (usage.getStatus() == CouponUsageStatus.RESERVED) {
                usage.setStatus(CouponUsageStatus.USED);
                usage.setUsedAt(now);
                usages.save(usage);
            }
        }
    }

    @Transactional
    public void releaseForCancellation(Order order, boolean preserveUsedHistory) {
        List<CouponUsage> reserved = usages.findAllByOrderIdForUpdate(order.getId()).stream()
            .filter(usage -> usage.getStatus() == CouponUsageStatus.RESERVED
                || (!preserveUsedHistory && usage.getStatus() == CouponUsageStatus.USED))
            .toList();
        for (CouponUsage usage : reserved.stream().sorted(Comparator.comparing(value -> value.getCoupon().getId())).toList()) {
            Coupon coupon = coupons.findByIdForUpdate(usage.getCoupon().getId()).orElse(null);
            if (coupon != null && coupon.getTimesUsed() > 0) {
                coupon.setTimesUsed(coupon.getTimesUsed() - 1);
                coupons.save(coupon);
            }
            usage.setStatus(CouponUsageStatus.RELEASED);
            usage.setReleasedAt(LocalDateTime.now());
            usages.save(usage);
        }
    }

    @Transactional(readOnly = true)
    public List<CouponUsageResponse> history(Long couponId) {
        return usages.findAllByCoupon_IdOrderByCreatedAtDescIdDesc(couponId).stream().map(usage ->
            new CouponUsageResponse(usage.getId(), usage.getUser()==null?null:usage.getUser().getId(), usage.getUser()==null?"Khách vãng lai":usage.getUser().getName(),
                usage.getOrder().getId(), usage.getDiscountAmount(), usage.getStatus().name(),
                usage.getCreatedAt(), usage.getUsedAt(), usage.getReleasedAt())).toList();
    }

    private Quote quote(Long userId, Long guestSessionId, List<CartItem> cart, BigDecimal subtotal, BigDecimal shippingFee,
                        String rawCouponCode, String rawFreeShippingCode, boolean lock) {
        String firstCode = normalizeNullable(rawCouponCode);
        String secondCode = normalizeNullable(rawFreeShippingCode);
        if (firstCode != null && firstCode.equals(secondCode)) {
            throw invalid("COUPON_DUPLICATED", "Một mã coupon không thể được áp dụng hai lần");
        }
        Map<String, Coupon> resolved = resolveAll(java.util.stream.Stream.of(firstCode, secondCode).filter(Objects::nonNull).toList(), lock);
        Coupon productCoupon = null;
        Coupon shippingCoupon = null;
        if (firstCode != null) {
            Coupon coupon = resolved.get(firstCode);
            if (coupon.getCouponType() == CouponType.FREESHIP) shippingCoupon = coupon;
            else productCoupon = coupon;
        }
        if (secondCode != null) {
            Coupon coupon = resolved.get(secondCode);
            if (coupon.getCouponType() != CouponType.FREESHIP) {
                throw invalid("COUPON_TYPE_MISMATCH", "Ô mã miễn phí vận chuyển chỉ chấp nhận coupon freeship");
            }
            if (shippingCoupon != null) {
                throw invalid("TOO_MANY_SHIPPING_COUPONS", "Mỗi đơn hàng chỉ được dùng một coupon miễn phí vận chuyển");
            }
            shippingCoupon = coupon;
        }

        BigDecimal eligibleProductSubtotal = validate(productCoupon, userId, guestSessionId, cart, subtotal);
        validate(shippingCoupon, userId, guestSessionId, cart, subtotal);
        BigDecimal productDiscount = productCoupon == null ? BigDecimal.ZERO : productDiscount(productCoupon, eligibleProductSubtotal);
        BigDecimal shippingDiscount = shippingCoupon == null ? BigDecimal.ZERO : cap(shippingFee, shippingCoupon.getMaxDiscountAmount());
        return new Quote(productCoupon, shippingCoupon, productDiscount, shippingDiscount,
            productCoupon == null ? null : description(productCoupon, productDiscount),
            shippingCoupon == null ? null : "Giảm phí vận chuyển " + shippingDiscount.setScale(0, RoundingMode.HALF_UP) + "đ");
    }

    private Map<String, Coupon> resolveAll(List<String> codes, boolean lock) {
        Map<String, Coupon> result = new HashMap<>();
        for (String code : codes.stream().distinct().sorted().toList()) {
            Coupon coupon = (lock ? coupons.findByCodeForUpdate(code) : coupons.findByCodeIgnoreCase(code))
                .orElseThrow(() -> invalid("COUPON_NOT_FOUND", "Mã giảm giá không tồn tại"));
            result.put(code, coupon);
        }
        return result;
    }

    private BigDecimal validate(Coupon coupon, Long userId, Long guestSessionId, List<CartItem> cart, BigDecimal subtotal) {
        if (coupon == null) return BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        if (!coupon.isActive()) throw invalid("COUPON_INACTIVE", "Mã giảm giá đang tạm ngừng");
        if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt())) throw invalid("COUPON_NOT_STARTED", "Mã giảm giá chưa tới thời gian sử dụng");
        if (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt())) throw invalid("COUPON_EXPIRED", "Mã giảm giá đã hết hạn");
        if (coupon.getMinimumOrderAmount() != null && subtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw invalid("COUPON_MINIMUM_NOT_MET", "Đơn hàng chưa đạt giá trị tối thiểu của coupon");
        }
        if (coupon.getUsageLimit() != null && coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            throw invalid("COUPON_USAGE_LIMIT_REACHED", "Mã giảm giá đã hết lượt sử dụng");
        }
        long customerUsage = userId != null
            ? usages.countByCoupon_IdAndUser_IdAndStatusIn(coupon.getId(), userId, OCCUPYING_STATUSES)
            : usages.countByCoupon_IdAndGuestSession_IdAndStatusIn(coupon.getId(), guestSessionId, OCCUPYING_STATUSES);
        if (coupon.getUsageLimitPerUser() != null && customerUsage >= coupon.getUsageLimitPerUser()) {
            throw invalid("COUPON_USER_LIMIT_REACHED", "Bạn đã sử dụng hết lượt của mã giảm giá này");
        }
        BigDecimal eligible = cart.stream().filter(item -> applies(coupon, item.getProduct()))
            .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        if (eligible.signum() == 0) throw invalid("COUPON_SCOPE_NOT_APPLICABLE", "Giỏ hàng không có sản phẩm phù hợp với coupon");
        return eligible;
    }

    private boolean applies(Coupon coupon, Product product) {
        return switch (coupon.getScopeType()) {
            case ALL -> true;
            case PRODUCT -> coupon.getProducts().stream().anyMatch(value -> value.getId().equals(product.getId()));
            case CATEGORY -> product.getCategory() != null && coupon.getCategories().stream().anyMatch(value -> value.getId().equals(product.getCategory().getId()));
        };
    }

    private BigDecimal productDiscount(Coupon coupon, BigDecimal eligibleSubtotal) {
        BigDecimal discount = coupon.getDiscountType() == DiscountType.PERCENTAGE
            ? eligibleSubtotal.multiply(BigDecimal.valueOf(coupon.getDiscountPercentage())).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            : Optional.ofNullable(coupon.getDiscountAmount()).orElse(BigDecimal.ZERO);
        return cap(discount.min(eligibleSubtotal), coupon.getMaxDiscountAmount());
    }

    private BigDecimal cap(BigDecimal amount, BigDecimal maximum) {
        return (maximum == null ? amount : amount.min(maximum)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private void reserveOne(User user, GuestSession guestSession, Order order, Coupon coupon, BigDecimal discount) {
        if (coupon == null) return;
        CouponUsage usage = new CouponUsage();
        usage.setCoupon(coupon); usage.setUser(user); usage.setGuestSession(guestSession); usage.setOrder(order); usage.setDiscountAmount(discount);
        usage.setStatus(CouponUsageStatus.RESERVED);
        usages.save(usage);
        coupon.setTimesUsed(coupon.getTimesUsed() + 1);
        coupons.save(coupon);
    }

    private String description(Coupon coupon, BigDecimal discount) {
        return coupon.getDiscountType() == DiscountType.PERCENTAGE
            ? "Giảm " + coupon.getDiscountPercentage() + "% (" + discount.setScale(0, RoundingMode.HALF_UP) + "đ)"
            : "Giảm " + discount.setScale(0, RoundingMode.HALF_UP) + "đ";
    }

    private String normalizeNullable(String code) {
        return StringUtils.hasText(code) ? code.trim().toUpperCase(Locale.ROOT) : null;
    }

    private ApplicationException invalid(String code, String message) {
        return new ApplicationException(HttpStatus.CONFLICT, code, message);
    }

    public record Quote(Coupon productCoupon, Coupon shippingCoupon, BigDecimal productDiscount,
                        BigDecimal shippingDiscount, String productDescription, String shippingDescription) {}
}
