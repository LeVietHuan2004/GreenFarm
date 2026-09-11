package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.OrderStatusUpdateRequest;
import com.agri.ecommerce.dto.response.OrderResponse;
import com.agri.ecommerce.dto.response.OrderSummaryResponse;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAuthority('manage_orders')")
public class OrderAdminController {
    private final OrderService service;

    public OrderAdminController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryResponse>> findAll(
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success("Lấy danh sách đơn hàng thành công", service.findAdminOrders(status, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> findOne(@PathVariable Long id) {
        return ApiResponse.success("Lấy chi tiết đơn hàng thành công", service.findAdminOrder(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", service.updateStatus(id, request.status(), request.note()));
    }
}
