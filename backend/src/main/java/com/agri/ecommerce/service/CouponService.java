package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.CouponRequest;
import com.agri.ecommerce.dto.response.CouponResponse;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.entity.Coupon;
import com.agri.ecommerce.entity.CouponType;
import com.agri.ecommerce.entity.DiscountType;
import com.agri.ecommerce.repository.CouponRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CouponService {
    private final CouponRepository coupons;

    public CouponService(CouponRepository coupons) {
        this.coupons = coupons;
    }

    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> findAll(String search, Boolean active, Pageable pageable) {
        Specification<Coupon> specification = (root, query, builder) -> {
            ArrayList<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(search)) {
                predicates.add(builder.like(builder.lower(root.get("code")), "%" + search.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (active != null) {
                predicates.add(builder.equal(root.get("active"), active));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.from(coupons.findAll(specification, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public CouponResponse findOne(Long id) {
        return toResponse(requireCoupon(id));
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        String code = normalizeCode(request.code());
        if (coupons.existsByCodeIgnoreCase(code)) {
            throw conflict("COUPON_CODE_EXISTS", "Mã giảm giá đã tồn tại");
        }
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setTimesUsed(0);
        apply(coupon, request);
        return toResponse(coupons.save(coupon));
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = requireCoupon(id);
        String code = normalizeCode(request.code());
        if (coupons.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw conflict("COUPON_CODE_EXISTS", "Mã giảm giá đã tồn tại");
        }
        coupon.setCode(code);
        apply(coupon, request);
        return toResponse(coupons.save(coupon));
    }

    @Transactional
    public CouponResponse updateActive(Long id, boolean active) {
        Coupon coupon = requireCoupon(id);
        coupon.setActive(active);
        return toResponse(coupons.save(coupon));
    }

    private void apply(Coupon coupon, CouponRequest request) {
        if (request.startsAt() != null && request.expiresAt() != null && !request.startsAt().isBefore(request.expiresAt())) {
            throw conflict("INVALID_COUPON_PERIOD", "Thời gian bắt đầu phải trước thời gian hết hạn");
        }

        coupon.setCouponType(request.couponType());
        coupon.setStartsAt(request.startsAt());
        coupon.setExpiresAt(request.expiresAt());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setActive(request.active());

        if (request.couponType() == CouponType.FREESHIP) {
            coupon.setDiscountType(DiscountType.PERCENTAGE);
            coupon.setDiscountPercentage(0);
            coupon.setDiscountAmount(null);
            return;
        }

        coupon.setDiscountType(request.discountType());
        if (request.discountType() == DiscountType.PERCENTAGE) {
            int percentage = request.discountPercentage() == null ? 0 : request.discountPercentage();
            if (percentage < 1 || percentage > 100) {
                throw conflict("INVALID_COUPON_VALUE", "Phần trăm giảm giá phải từ 1 đến 100");
            }
            coupon.setDiscountPercentage(percentage);
            coupon.setDiscountAmount(null);
            return;
        }

        BigDecimal amount = request.discountAmount();
        if (amount == null || amount.signum() <= 0) {
            throw conflict("INVALID_COUPON_VALUE", "Số tiền giảm phải lớn hơn 0");
        }
        coupon.setDiscountPercentage(0);
        coupon.setDiscountAmount(amount);
    }

    private Coupon requireCoupon(Long id) {
        return coupons.findById(id).orElseThrow(() -> new ApplicationException(
            HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "Không tìm thấy mã giảm giá"
        ));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private boolean currentlyUsable(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        return coupon.isActive()
            && (coupon.getStartsAt() == null || !now.isBefore(coupon.getStartsAt()))
            && (coupon.getExpiresAt() == null || !now.isAfter(coupon.getExpiresAt()))
            && (coupon.getUsageLimit() == null || coupon.getTimesUsed() < coupon.getUsageLimit());
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
            coupon.getId(), coupon.getCode(), coupon.getCouponType().name(), coupon.getDiscountType().name(),
            coupon.getDiscountPercentage(), coupon.getDiscountAmount(), coupon.getStartsAt(), coupon.getExpiresAt(),
            coupon.getUsageLimit(), coupon.getTimesUsed(), coupon.isActive(), currentlyUsable(coupon),
            coupon.getCreatedAt(), coupon.getUpdatedAt()
        );
    }

    private ApplicationException conflict(String code, String message) {
        return new ApplicationException(HttpStatus.CONFLICT, code, message);
    }
}
