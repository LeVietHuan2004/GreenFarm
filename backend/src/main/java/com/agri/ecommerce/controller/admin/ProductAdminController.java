package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.ProductImageRequest;
import com.agri.ecommerce.dto.request.ProductRequest;
import com.agri.ecommerce.dto.request.ProductStatusRequest;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.dto.response.ProductImageResponse;
import com.agri.ecommerce.dto.response.ProductResponse;
import com.agri.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class ProductAdminController {

    private final ProductService productService;

    public ProductAdminController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> findProducts(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        return ApiResponse.success(
            "Lay danh sach san pham quan tri thanh cong",
            productService.findAdminProducts(search, categoryId, status, pageable)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("Tao san pham thanh cong", productService.create(request));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> update(
        @PathVariable Long productId,
        @Valid @RequestBody ProductRequest request
    ) {
        return ApiResponse.success(
            "Cap nhat san pham thanh cong",
            productService.update(productId, request)
        );
    }

    @PatchMapping("/{productId}/status")
    public ApiResponse<ProductResponse> updateStatus(
        @PathVariable Long productId,
        @Valid @RequestBody ProductStatusRequest request
    ) {
        return ApiResponse.success(
            "Cap nhat trang thai san pham thanh cong",
            productService.updateStatus(productId, request.status())
        );
    }

    @PostMapping("/{productId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductImageResponse> addImage(
        @PathVariable Long productId,
        @Valid @RequestBody ProductImageRequest request
    ) {
        return ApiResponse.success(
            "Them anh san pham thanh cong",
            productService.addImage(productId, request)
        );
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ApiResponse<Void> deleteImage(
        @PathVariable Long productId,
        @PathVariable Long imageId
    ) {
        productService.deleteImage(productId, imageId);
        return ApiResponse.success("Xoa anh san pham thanh cong", null);
    }
}
