package com.agri.ecommerce.controller.auth;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.LoginRequest;
import com.agri.ecommerce.dto.request.RegisterRequest;
import com.agri.ecommerce.dto.response.AuthResponse;
import com.agri.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Dang ky tai khoan thanh cong", authService.register(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Dang nhap thanh cong", authService.login(request));
    }
}
