package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.CouponRequest;
import com.agri.ecommerce.dto.request.CouponStatusRequest;
import com.agri.ecommerce.dto.response.CouponResponse;
import com.agri.ecommerce.dto.response.CouponUsageResponse;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasAuthority('manage_coupons')")
public class CouponAdminController {
    private final CouponService service;

    public CouponAdminController(CouponService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<CouponResponse>> findAll(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Boolean active,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success("Lấy danh sách mã giảm giá thành công", service.findAll(search, active, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<CouponResponse> findOne(@PathVariable Long id) {
        return ApiResponse.success("Lấy mã giảm giá thành công", service.findOne(id));
    }

    @GetMapping("/{id}/usages")
    public ApiResponse<List<CouponUsageResponse>> history(@PathVariable Long id) {
        return ApiResponse.success("Lấy lịch sử sử dụng coupon thành công", service.history(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.success("Tạo mã giảm giá thành công", service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CouponResponse> update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ApiResponse.success("Cập nhật mã giảm giá thành công", service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<CouponResponse> updateActive(@PathVariable Long id, @Valid @RequestBody CouponStatusRequest request) {
        return ApiResponse.success("Cập nhật trạng thái mã giảm giá thành công", service.updateActive(id, request.active()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success("Xóa mã giảm giá thành công", null);
    }
}
