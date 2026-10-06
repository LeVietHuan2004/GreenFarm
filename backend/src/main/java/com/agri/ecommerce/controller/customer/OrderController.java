package com.agri.ecommerce.controller.customer;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.CheckoutRequest;
import com.agri.ecommerce.dto.request.CustomerRefundRequest;
import com.agri.ecommerce.dto.response.OrderResponse;
import com.agri.ecommerce.dto.response.RefundRequestResponse;
import com.agri.ecommerce.dto.response.OrderSummaryResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.OrderService;
import com.agri.ecommerce.service.RefundRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {
    private final OrderService service;
    private final RefundRequestService refunds;
    public OrderController(OrderService service, RefundRequestService refunds){this.service=service;this.refunds=refunds;}
    @PostMapping public ApiResponse<OrderResponse> create(@AuthenticationPrincipal GreenFarmUserDetails user,@Valid @RequestBody CheckoutRequest request, HttpServletRequest servletRequest){return ApiResponse.success("Đặt hàng thành công",service.create(user.userId(),request,clientIp(servletRequest)));}
    @GetMapping public ApiResponse<List<OrderSummaryResponse>> findAll(@AuthenticationPrincipal GreenFarmUserDetails user){return ApiResponse.success("Lấy danh sách đơn hàng thành công",service.findAll(user.userId()));}
    @GetMapping("/{id}") public ApiResponse<OrderResponse> findOne(@AuthenticationPrincipal GreenFarmUserDetails user,@PathVariable Long id){return ApiResponse.success("Lấy chi tiết đơn hàng thành công",service.findOne(user.userId(),id));}
    @PatchMapping("/{id}/cancel") public ApiResponse<OrderResponse> cancel(@AuthenticationPrincipal GreenFarmUserDetails user,@PathVariable Long id){return ApiResponse.success("Hủy đơn hàng thành công",service.cancel(user.userId(),id));}
    @PostMapping("/{id}/refund-requests") public ApiResponse<RefundRequestResponse> requestRefund(@AuthenticationPrincipal GreenFarmUserDetails user,@PathVariable Long id,@Valid @RequestBody CustomerRefundRequest request){return ApiResponse.success("Yeu cau hoan tien da duoc gui",refunds.create(user.userId(),id,request.reason(),request.details()));}
    @GetMapping("/{id}/refund-request") public ApiResponse<RefundRequestResponse> latestRefundRequest(@AuthenticationPrincipal GreenFarmUserDetails user,@PathVariable Long id){return ApiResponse.success("Lay yeu cau hoan tien",refunds.latestForCustomer(user.userId(),id));}
    private String clientIp(HttpServletRequest request) { String forwarded=request.getHeader("X-Forwarded-For"); return forwarded==null||forwarded.isBlank()?request.getRemoteAddr():forwarded.split(",")[0].trim(); }
}
