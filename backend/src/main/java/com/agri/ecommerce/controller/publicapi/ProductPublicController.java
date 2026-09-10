package com.agri.ecommerce.controller.publicapi;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.dto.response.ProductResponse;
import com.agri.ecommerce.service.ProductService;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/products")
public class ProductPublicController {

    private final ProductService productService;

    public ProductPublicController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> findProducts(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        return ApiResponse.success(
            "Lay danh sach san pham thanh cong",
            productService.findPublicProducts(
                search,
                category,
                minPrice,
                maxPrice,
                status,
                pageable
            )
        );
    }

    @GetMapping("/{slug}")
    public ApiResponse<ProductResponse> findProduct(@PathVariable String slug) {
        return ApiResponse.success(
            "Lay chi tiet san pham thanh cong",
            productService.findPublicProduct(slug)
        );
    }
}
