package com.agri.ecommerce.controller.publicapi;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.service.ReviewService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/products/{productId}/reviews")
public class ReviewPublicController {
    private final ReviewService service;
    public ReviewPublicController(ReviewService service) { this.service = service; }
    @GetMapping public ApiResponse<PageResponse<ReviewResponse>> findAll(@PathVariable Long productId, @PageableDefault(size=10) Pageable pageable) {
        return ApiResponse.success("Lấy đánh giá sản phẩm thành công", service.findByProduct(productId, pageable));
    }
}
