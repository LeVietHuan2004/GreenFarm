package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.CouponRequest;
import com.agri.ecommerce.dto.response.CouponResponse;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CouponService {
    private final CouponRepository coupons;
    private final CouponUsageRepository usages;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final CouponEngineService couponEngine;

    public CouponService(CouponRepository coupons, CouponUsageRepository usages, CategoryRepository categories,
                         ProductRepository products, CouponEngineService couponEngine) {
        this.coupons = coupons;
        this.usages = usages;
        this.categories = categories;
        this.products = products;
        this.couponEngine = couponEngine;
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
        if ((coupon.getTimesUsed() > 0 || usages.existsByCoupon_Id(id)) && changesProtectedFields(coupon, request)) {
            throw conflict("COUPON_IMMUTABLE_AFTER_USE", "Coupon đã được sử dụng; không thể đổi loại, giá trị giảm hoặc phạm vi áp dụng");
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

    @Transactional
    public void delete(Long id) {
        Coupon coupon = requireCoupon(id);
        if (coupon.getTimesUsed() > 0 || usages.existsByCoupon_Id(id)) {
            throw conflict("COUPON_IN_USE", "Coupon đã có lịch sử sử dụng; chỉ có thể tạm dừng, không thể xóa");
        }
        coupons.delete(coupon);
    }

    @Transactional(readOnly = true)
    public List<com.agri.ecommerce.dto.response.CouponUsageResponse> history(Long id) {
        requireCoupon(id);
        return couponEngine.history(id);
    }

    private void apply(Coupon coupon, CouponRequest request) {
        if (request.startsAt() != null && request.expiresAt() != null && !request.startsAt().isBefore(request.expiresAt())) {
            throw conflict("INVALID_COUPON_PERIOD", "Thời gian bắt đầu phải trước thời gian hết hạn");
        }

        coupon.setCouponType(request.couponType());
        coupon.setName(request.name().trim());
        coupon.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        coupon.setStartsAt(request.startsAt());
        coupon.setExpiresAt(request.expiresAt());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setUsageLimitPerUser(request.usageLimitPerUser());
        coupon.setMinimumOrderAmount(request.minimumOrderAmount());
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        coupon.setScopeType(request.scopeType());
        coupon.setActive(request.active());
        applyScope(coupon, request);

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

    private void applyScope(Coupon coupon, CouponRequest request) {
        coupon.getCategories().clear();
        coupon.getProducts().clear();
        if (request.scopeType() == CouponScopeType.ALL) return;
        if (request.scopeType() == CouponScopeType.CATEGORY) {
            Set<Long> ids = request.categoryIds() == null ? Set.of() : request.categoryIds();
            List<Category> selected = categories.findAllById(ids);
            if (ids.isEmpty() || selected.size() != ids.size()) throw conflict("INVALID_COUPON_SCOPE", "Danh mục áp dụng không hợp lệ");
            coupon.getCategories().addAll(selected);
            return;
        }
        Set<Long> ids = request.productIds() == null ? Set.of() : request.productIds();
        List<Product> selected = products.findAllById(ids);
        if (ids.isEmpty() || selected.size() != ids.size()) throw conflict("INVALID_COUPON_SCOPE", "Sản phẩm áp dụng không hợp lệ");
        coupon.getProducts().addAll(selected);
    }

    private boolean changesProtectedFields(Coupon coupon, CouponRequest request) {
        Set<Long> categoryIds = request.categoryIds() == null ? Set.of() : request.categoryIds();
        Set<Long> productIds = request.productIds() == null ? Set.of() : request.productIds();
        return coupon.getCouponType() != request.couponType()
            || coupon.getDiscountType() != normalizedDiscountType(request)
            || coupon.getDiscountPercentage() != normalizedPercentage(request)
            || !sameAmount(coupon.getDiscountAmount(), normalizedAmount(request))
            || !sameAmount(coupon.getMaxDiscountAmount(), request.maxDiscountAmount())
            || coupon.getScopeType() != request.scopeType()
            || !ids(coupon.getCategories()).equals(categoryIds)
            || !ids(coupon.getProducts()).equals(productIds);
    }

    private DiscountType normalizedDiscountType(CouponRequest request) { return request.couponType() == CouponType.FREESHIP ? DiscountType.PERCENTAGE : request.discountType(); }
    private int normalizedPercentage(CouponRequest request) { return request.couponType() == CouponType.FREESHIP ? 0 : request.discountType() == DiscountType.PERCENTAGE ? Objects.requireNonNullElse(request.discountPercentage(), 0) : 0; }
    private BigDecimal normalizedAmount(CouponRequest request) { return request.couponType() == CouponType.ORDER_DISCOUNT && request.discountType() == DiscountType.FIXED_AMOUNT ? request.discountAmount() : null; }
    private boolean sameAmount(BigDecimal first, BigDecimal second) {
        return first == null ? second == null : second != null && first.compareTo(second) == 0;
    }
    private Set<Long> ids(Set<?> values) { return values.stream().map(value -> value instanceof Category category ? category.getId() : ((Product)value).getId()).collect(Collectors.toSet()); }

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
            coupon.getId(), coupon.getCode(), coupon.getName(), coupon.getDescription(), coupon.getCouponType().name(), coupon.getDiscountType().name(),
            coupon.getDiscountPercentage(), coupon.getDiscountAmount(), coupon.getMaxDiscountAmount(), coupon.getMinimumOrderAmount(), coupon.getScopeType().name(),
            coupon.getCategories().stream().map(Category::getId).sorted().toList(), coupon.getProducts().stream().map(Product::getId).sorted().toList(),
            coupon.getStartsAt(), coupon.getExpiresAt(), coupon.getUsageLimit(), coupon.getUsageLimitPerUser(), coupon.getTimesUsed(), coupon.isActive(), currentlyUsable(coupon),
            coupon.getCreatedAt(), coupon.getUpdatedAt()
        );
    }

    private ApplicationException conflict(String code, String message) {
        return new ApplicationException(HttpStatus.CONFLICT, code, message);
    }
}
