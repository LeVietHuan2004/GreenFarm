package com.agri.ecommerce.controller.staff;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.OrderStatusUpdateRequest;
import com.agri.ecommerce.dto.response.OrderResponse;
import com.agri.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/orders")
@PreAuthorize("hasAuthority('manage_orders')")
public class StaffOrderController {
    private final OrderService service;

    public StaffOrderController(OrderService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<OrderResponse>> findAll() {
        return ApiResponse.success("Lay danh sach don can xu ly thanh cong", service.findStaffOrders());
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ApiResponse.success("Cap nhat trang thai don hang thanh cong", service.updateStaffStatus(id, request.status(), request.note()));
    }
}
