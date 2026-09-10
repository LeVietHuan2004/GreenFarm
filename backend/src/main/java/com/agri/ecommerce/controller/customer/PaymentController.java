package com.agri.ecommerce.controller.customer;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.PaymentMethodOptionsResponse;
import com.agri.ecommerce.service.PaymentService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) { this.service = service; }

    @GetMapping("/methods")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<PaymentMethodOptionsResponse> methods() {
        return ApiResponse.success("Lấy phương thức thanh toán thành công", service.options());
    }

    @GetMapping("/vnpay/return")
    public void vnpayReturn(@RequestParam Map<String, String> parameters, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader("Location", service.resultRedirectUrl(service.handleVnpayCallback(parameters)));
    }

    @GetMapping("/vnpay/ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> parameters) {
        return service.ipnResponse(parameters);
    }
}
