package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.LoginRequest;
import com.agri.ecommerce.dto.request.RegisterRequest;
import com.agri.ecommerce.dto.response.AuthResponse;
import com.agri.ecommerce.entity.Role;
import com.agri.ecommerce.entity.User;
import com.agri.ecommerce.entity.UserStatus;
import com.agri.ecommerce.mapper.UserMapper;
import com.agri.ecommerce.repository.RoleRepository;
import com.agri.ecommerce.repository.UserRepository;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.security.JwtService;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        LoginAttemptService loginAttemptService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApplicationException(
                HttpStatus.CONFLICT,
                "EMAIL_EXISTS",
                "Email da duoc su dung"
            );
        }

        Role customerRole = roleRepository.findByNameIgnoreCase("customer")
            .orElseThrow(() -> new ApplicationException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "DEFAULT_ROLE_NOT_FOUND",
                "He thong chua duoc cau hinh vai tro khach hang"
            ));

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(trimToNull(request.phoneNumber()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(customerRole);
        user.setLastLoginAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        return createAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
            .orElseThrow(this::invalidCredentials);
        ensureAccountCanLogin(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            LoginAttemptService.LoginAttemptResult result =
                loginAttemptService.recordFailure(user.getId());
            if (result.locked()) {
                throw new ApplicationException(
                    HttpStatus.LOCKED,
                    "ACCOUNT_LOCKED",
                    "Tai khoan tam khoa do dang nhap sai nhieu lan, vui long thu lai sau"
                );
            }
            throw new ApplicationException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Email hoac mat khau khong dung. Con "
                    + result.remainingAttempts() + " lan thu"
            );
        }

        ensureRequestedRoleMatches(user, request.role());
        User authenticatedUser = loginAttemptService.recordSuccess(user.getId());
        return createAuthResponse(authenticatedUser);
    }

    private void ensureRequestedRoleMatches(User user, String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return;
        }

        String actualRole = user.getRole().getName();
        if (!actualRole.equalsIgnoreCase(requestedRole.trim())) {
            throw new ApplicationException(
                HttpStatus.FORBIDDEN,
                "ROLE_MISMATCH",
                "Tai khoan khong thuoc cong dang nhap da chon"
            );
        }
    }

    private void ensureAccountCanLogin(User user) {
        if (user.isTemporarilyLocked(LocalDateTime.now())) {
            throw new ApplicationException(
                HttpStatus.LOCKED,
                "ACCOUNT_LOCKED",
                "Tai khoan dang tam khoa, vui long thu lai sau"
            );
        }

        switch (user.getStatus()) {
            case ACTIVE -> {
            }
            case PENDING -> throw new ApplicationException(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_PENDING",
                "Tai khoan chua duoc kich hoat"
            );
            case BANNED -> throw new ApplicationException(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_BANNED",
                "Tai khoan da bi khoa"
            );
            case DELETED -> throw invalidCredentials();
        }
    }

    private AuthResponse createAuthResponse(User user) {
        String token = jwtService.generateToken(GreenFarmUserDetails.from(user));
        return new AuthResponse(
            token,
            "Bearer",
            jwtService.getExpirationSeconds(),
            UserMapper.toResponse(user)
        );
    }

    private ApplicationException invalidCredentials() {
        return new ApplicationException(
            HttpStatus.UNAUTHORIZED,
            "INVALID_CREDENTIALS",
            "Email hoac mat khau khong dung"
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
