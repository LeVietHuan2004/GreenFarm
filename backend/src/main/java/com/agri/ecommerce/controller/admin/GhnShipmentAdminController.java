package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.ShipmentResponse;
import com.agri.ecommerce.dto.response.ShipmentEventResponse;
import java.util.List;
import com.agri.ecommerce.service.GhnShipmentService;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders/{orderId}/ghn")
@PreAuthorize("hasRole('ADMIN')")
public class GhnShipmentAdminController {
    private final GhnShipmentService service;
    public GhnShipmentAdminController(GhnShipmentService service){this.service=service;}
    @PostMapping("/create") public ApiResponse<ShipmentResponse> create(@PathVariable Long orderId){return ApiResponse.success("Đã tạo vận đơn GHN",service.create(orderId));}
    @PostMapping("/sync") public ApiResponse<ShipmentResponse> sync(@PathVariable Long orderId){return ApiResponse.success("Đã đồng bộ GHN",service.sync(orderId));}
    @PostMapping("/cancel") public ApiResponse<ShipmentResponse> cancel(@PathVariable Long orderId){return ApiResponse.success("Đã hủy vận đơn GHN",service.cancel(orderId));}
    @GetMapping("/print") public ApiResponse<Map<String,String>> print(@PathVariable Long orderId){return ApiResponse.success("Lấy phiếu gửi GHN",Map.of("url",service.printUrl(orderId)));}
    @GetMapping("/events") public ApiResponse<List<ShipmentEventResponse>> events(@PathVariable Long orderId){return ApiResponse.success("Lịch sử vận đơn GHN",service.events(orderId));}
}
