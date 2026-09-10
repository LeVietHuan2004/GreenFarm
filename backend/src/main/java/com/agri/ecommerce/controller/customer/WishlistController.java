package com.agri.ecommerce.controller.customer;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.WishlistItemRequest;
import com.agri.ecommerce.dto.response.WishlistResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
@PreAuthorize("hasRole('CUSTOMER')")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ApiResponse<WishlistResponse> getWishlist(
        @AuthenticationPrincipal GreenFarmUserDetails principal
    ) {
        return ApiResponse.success(
            "Lay danh sach yeu thich thanh cong",
            wishlistService.getWishlist(principal.userId())
        );
    }

    @PostMapping("/items")
    public ApiResponse<WishlistResponse> addItem(
        @AuthenticationPrincipal GreenFarmUserDetails principal,
        @Valid @RequestBody WishlistItemRequest request
    ) {
        return ApiResponse.success(
            "Them san pham yeu thich thanh cong",
            wishlistService.addItem(principal.userId(), request)
        );
    }

    @DeleteMapping("/items/{productId}")
    public ApiResponse<WishlistResponse> removeItem(
        @AuthenticationPrincipal GreenFarmUserDetails principal,
        @PathVariable Long productId
    ) {
        return ApiResponse.success(
            "Xoa san pham yeu thich thanh cong",
            wishlistService.removeItem(principal.userId(), productId)
        );
    }
}
