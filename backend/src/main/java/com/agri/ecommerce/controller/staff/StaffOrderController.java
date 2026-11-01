package com.agri.ecommerce.controller.staff;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.OrderStatusUpdateRequest;
import com.agri.ecommerce.dto.response.OrderResponse;
import com.agri.ecommerce.service.OrderService;
import com.agri.ecommerce.dto.request.DeliveryAssignmentRequest;
import com.agri.ecommerce.entity.UserStatus;
import com.agri.ecommerce.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/orders")
@PreAuthorize("hasAuthority('manage_orders')")
public class StaffOrderController {
    private final OrderService service;
    private final UserRepository users;

    public StaffOrderController(OrderService service, UserRepository users) { this.service = service; this.users = users; }

    public record DeliveryStaff(Long id, String name) {}
    @GetMapping("/delivery-staff")
    public ApiResponse<List<DeliveryStaff>> deliveryStaff() {
        return ApiResponse.success("Nhân viên giao hàng", users.findAllByRoleAndStatus("delivery_staff", UserStatus.ACTIVE).stream().map(user -> new DeliveryStaff(user.getId(), user.getName())).toList());
    }

    @PatchMapping("/{id}/delivery-staff")
    public ApiResponse<OrderResponse> assign(@PathVariable Long id, @Valid @RequestBody DeliveryAssignmentRequest request) {
        return ApiResponse.success("Đã phân công giao hàng", service.assignDeliveryStaff(id, request.deliveryStaffId()));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> findAll() {
        return ApiResponse.success("Lay danh sach don can xu ly thanh cong", service.findStaffOrders());
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ApiResponse.success("Cap nhat trang thai don hang thanh cong", service.updateStaffStatus(id, request.status(), request.note()));
    }
}
