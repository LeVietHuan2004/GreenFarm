package com.agri.ecommerce.controller.customer;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.ChangePasswordRequest;
import com.agri.ecommerce.dto.request.UpdateProfileRequest;
import com.agri.ecommerce.dto.response.UserResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<UserResponse> getProfile(
        @AuthenticationPrincipal GreenFarmUserDetails principal
    ) {
        return ApiResponse.success(
            "Lay thong tin tai khoan thanh cong",
            userService.getCurrentUser(principal.getUsername())
        );
    }

    @PatchMapping
    public ApiResponse<UserResponse> updateProfile(
        @AuthenticationPrincipal GreenFarmUserDetails principal,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success(
            "Cap nhat thong tin thanh cong",
            userService.updateProfile(principal.getUsername(), request)
        );
    }

    @PutMapping("/password")
    public ApiResponse<Map<String, Boolean>> changePassword(
        @AuthenticationPrincipal GreenFarmUserDetails principal,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(principal.getUsername(), request);
        return ApiResponse.success("Doi mat khau thanh cong", Map.of("changed", true));
    }
}
