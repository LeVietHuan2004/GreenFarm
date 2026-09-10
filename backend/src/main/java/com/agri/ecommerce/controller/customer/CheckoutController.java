package com.agri.ecommerce.controller.customer;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.CheckoutRequest;
import com.agri.ecommerce.dto.response.CheckoutPreviewResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@PreAuthorize("hasRole('CUSTOMER')")
public class CheckoutController {
    private final OrderService service;
    public CheckoutController(OrderService service){this.service=service;}
    @PostMapping("/preview") public ApiResponse<CheckoutPreviewResponse> preview(@AuthenticationPrincipal GreenFarmUserDetails user,@Valid @RequestBody CheckoutRequest request){return ApiResponse.success("Tính đơn hàng thành công",service.preview(user.userId(),request));}
}
