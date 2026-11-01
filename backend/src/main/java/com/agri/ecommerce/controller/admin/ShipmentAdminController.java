package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.AdminShipmentResponse;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.service.AdminShipmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/shipments")
@PreAuthorize("hasRole('ADMIN')")
public class ShipmentAdminController {
    private final AdminShipmentService service;
    public ShipmentAdminController(AdminShipmentService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResponse<AdminShipmentResponse>> findAll(
        @RequestParam(required = false) String search, @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Lấy danh sách vận đơn thành công", service.findAll(search, status, page, size));
    }
}
