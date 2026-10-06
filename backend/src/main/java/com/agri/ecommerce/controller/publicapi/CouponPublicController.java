package com.agri.ecommerce.controller.publicapi;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.AvailableCouponResponse;
import com.agri.ecommerce.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/coupons")
public class CouponPublicController {
    private final CouponRepository coupons;

    public CouponPublicController(CouponRepository coupons) {
        this.coupons = coupons;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<AvailableCouponResponse>> findAvailable() {
        List<AvailableCouponResponse> available = coupons.findPubliclyAvailable(LocalDateTime.now()).stream()
            .map(coupon -> new AvailableCouponResponse(
                coupon.getCode(), coupon.getName(), coupon.getDescription(), coupon.getCouponType().name(),
                coupon.getDiscountType().name(), coupon.getDiscountPercentage(), coupon.getDiscountAmount(),
                coupon.getMaxDiscountAmount(), coupon.getMinimumOrderAmount(), coupon.getScopeType().name(),
                coupon.getExpiresAt()
            ))
            .toList();
        return ApiResponse.success("Lấy danh sách mã giảm giá thành công", available);
    }
}
