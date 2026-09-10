package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.AdminUpdateUserRequest;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.dto.response.UserResponse;
import com.agri.ecommerce.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> findUsers(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ApiResponse.success(
            "Lay danh sach nguoi dung thanh cong",
            userService.findUsers(search, role, status, pageable)
        );
    }

    @PatchMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(
        @PathVariable Long userId,
        @Valid @RequestBody AdminUpdateUserRequest request,
        Principal principal
    ) {
        return ApiResponse.success(
            "Cap nhat nguoi dung thanh cong",
            userService.updateUser(userId, request, principal.getName())
        );
    }
}
