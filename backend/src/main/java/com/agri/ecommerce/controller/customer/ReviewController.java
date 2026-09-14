package com.agri.ecommerce.controller.customer;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.ReviewRequest;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService service;
    public ReviewController(ReviewService service) { this.service = service; }
    @GetMapping("/eligibility/{productId}") public ApiResponse<ReviewEligibilityResponse> eligibility(@AuthenticationPrincipal GreenFarmUserDetails user, @PathVariable Long productId) { return ApiResponse.success("Kiểm tra quyền đánh giá thành công", service.eligibility(user.userId(), productId)); }
    @PostMapping public ApiResponse<ReviewResponse> create(@AuthenticationPrincipal GreenFarmUserDetails user, @Valid @RequestBody ReviewRequest request) { return ApiResponse.success("Đã gửi đánh giá", service.create(user.userId(), request)); }
    @PutMapping("/{id}") public ApiResponse<ReviewResponse> update(@AuthenticationPrincipal GreenFarmUserDetails user, @PathVariable Long id, @Valid @RequestBody ReviewRequest request) { return ApiResponse.success("Đã cập nhật đánh giá", service.update(user.userId(), id, request)); }
}
