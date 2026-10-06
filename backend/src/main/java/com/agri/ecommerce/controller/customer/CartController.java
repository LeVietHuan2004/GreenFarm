package com.agri.ecommerce.controller.customer;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.CartItemRequest;
import com.agri.ecommerce.dto.request.MergeCartRequest;
import com.agri.ecommerce.dto.request.UpdateCartItemRequest;
import com.agri.ecommerce.dto.response.CartResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.CartService;
import com.agri.ecommerce.service.GuestCommerceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;
    private final GuestCommerceService guestCommerceService;

    public CartController(CartService cartService, GuestCommerceService guestCommerceService) {
        this.cartService = cartService;
        this.guestCommerceService = guestCommerceService;
    }

    @PostMapping("/merge-guest")
    public ApiResponse<CartResponse> mergeGuest(@AuthenticationPrincipal GreenFarmUserDetails principal,@RequestHeader("X-Guest-Token") String guestToken) {
        return ApiResponse.success("Đã hợp nhất giỏ hàng",guestCommerceService.mergeIntoCustomer(guestToken,principal.userId()));
    }

    @GetMapping
    public ApiResponse<CartResponse> getCart(
        @AuthenticationPrincipal GreenFarmUserDetails principal
    ) {
        return ApiResponse.success(
            "Lay gio hang thanh cong",
            cartService.getCart(principal.userId())
        );
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(
        @AuthenticationPrincipal GreenFarmUserDetails principal,
        @Valid @RequestBody CartItemRequest request
    ) {
        return ApiResponse.success(
            "Them san pham vao gio hang thanh cong",
            cartService.addItem(principal.userId(), request)
        );
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItem(
        @AuthenticationPrincipal GreenFarmUserDetails principal,
        @PathVariable Long itemId,
        @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ApiResponse.success(
            "Cap nhat gio hang thanh cong",
            cartService.updateItem(principal.userId(), itemId, request)
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(
        @AuthenticationPrincipal GreenFarmUserDetails principal,
        @PathVariable Long itemId
    ) {
        return ApiResponse.success(
            "Xoa san pham khoi gio hang thanh cong",
            cartService.removeItem(principal.userId(), itemId)
        );
    }

    @PostMapping("/merge")
    public ApiResponse<CartResponse> mergeCart(
        @AuthenticationPrincipal GreenFarmUserDetails principal,
        @Valid @RequestBody MergeCartRequest request
    ) {
        return ApiResponse.success(
            "Dong bo gio hang thanh cong",
            cartService.mergeCart(principal.userId(), request)
        );
    }

    @DeleteMapping
    public ApiResponse<CartResponse> clearCart(
        @AuthenticationPrincipal GreenFarmUserDetails principal
    ) {
        return ApiResponse.success(
            "Xoa gio hang thanh cong",
            cartService.clearCart(principal.userId())
        );
    }
}
