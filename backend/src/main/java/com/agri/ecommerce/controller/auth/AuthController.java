package com.agri.ecommerce.controller.auth;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.LoginRequest;
import com.agri.ecommerce.dto.request.RegisterRequest;
import com.agri.ecommerce.dto.request.RefreshTokenRequest;
import com.agri.ecommerce.dto.response.AuthResponse;
import com.agri.ecommerce.service.AuthService;
import com.agri.ecommerce.service.PasswordResetService;
import com.agri.ecommerce.dto.request.ForgotPasswordRequest;
import com.agri.ecommerce.dto.request.ResetPasswordRequest;
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
    private final PasswordResetService passwordReset;

    public AuthController(AuthService authService, PasswordResetService passwordReset) {
        this.authService = authService;
        this.passwordReset = passwordReset;
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordReset.request(request.email());
        return ApiResponse.success("Nếu email hợp lệ, hướng dẫn đặt lại mật khẩu sẽ được gửi.", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordReset.reset(request.token(), request.newPassword());
        return ApiResponse.success("Đã đặt lại mật khẩu. Hãy đăng nhập lại.", null);
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

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("Làm mới phiên đăng nhập thành công", authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success("Đăng xuất thành công", null);
    }
}
