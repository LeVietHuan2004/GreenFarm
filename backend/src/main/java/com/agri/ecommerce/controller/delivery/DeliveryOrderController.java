package com.agri.ecommerce.controller.delivery;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.DeliveryStatusUpdateRequest;
import com.agri.ecommerce.dto.response.OrderResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery/orders")
@PreAuthorize("hasAuthority('manage_deliveries')")
public class DeliveryOrderController {
    private final OrderService service;

    public DeliveryOrderController(OrderService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<OrderResponse>> findAll(@AuthenticationPrincipal GreenFarmUserDetails user) {
        return ApiResponse.success("Lay danh sach don giao hang thanh cong", service.findDeliveryOrders(user.userId()));
    }

    @PostMapping("/{id}/claim")
    public ApiResponse<OrderResponse> claim(@AuthenticationPrincipal GreenFarmUserDetails user, @PathVariable Long id) {
        return ApiResponse.success("Da nhan don giao hang", service.claimForDelivery(id, user.userId()));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@AuthenticationPrincipal GreenFarmUserDetails user, @PathVariable Long id, @Valid @RequestBody DeliveryStatusUpdateRequest request) {
        return ApiResponse.success("Cap nhat trang thai giao hang thanh cong", service.updateDeliveryStatus(id, user.userId(), request.status(), request.note()));
    }
}
