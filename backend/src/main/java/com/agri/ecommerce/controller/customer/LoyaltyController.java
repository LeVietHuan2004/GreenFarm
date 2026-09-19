package com.agri.ecommerce.controller.customer;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.LoyaltySummaryResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.LoyaltyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loyalty")
@PreAuthorize("hasRole('CUSTOMER')")
public class LoyaltyController {
    private final LoyaltyService service;
    public LoyaltyController(LoyaltyService service) { this.service = service; }
    @GetMapping public ApiResponse<LoyaltySummaryResponse> summary(@AuthenticationPrincipal GreenFarmUserDetails user) {
        return ApiResponse.success("Lấy điểm tích lũy thành công", service.summary(user.userId()));
    }
}
