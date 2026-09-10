package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.AdminUpdateUserRequest;
import com.agri.ecommerce.dto.request.ChangePasswordRequest;
import com.agri.ecommerce.dto.request.UpdateProfileRequest;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.dto.response.UserResponse;
import com.agri.ecommerce.entity.Role;
import com.agri.ecommerce.entity.User;
import com.agri.ecommerce.entity.UserStatus;
import com.agri.ecommerce.mapper.UserMapper;
import com.agri.ecommerce.repository.RoleRepository;
import com.agri.ecommerce.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return UserMapper.toResponse(findByEmail(email));
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);
        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(trimToNull(request.phoneNumber()));
        }
        if (request.address() != null) {
            user.setAddress(trimToNull(request.address()));
        }
        if (request.avatar() != null) {
            user.setAvatar(trimToNull(request.avatar()));
        }
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApplicationException(
                HttpStatus.BAD_REQUEST,
                "CURRENT_PASSWORD_INVALID",
                "Mat khau hien tai khong dung"
            );
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ApplicationException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_REUSED",
                "Mat khau moi phai khac mat khau hien tai"
            );
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findUsers(
        String search,
        String role,
        String status,
        Pageable pageable
    ) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        String normalizedRole = StringUtils.hasText(role) ? role.trim() : null;
        UserStatus parsedStatus = StringUtils.hasText(status)
            ? UserStatus.from(status.trim())
            : null;
        return PageResponse.from(
            userRepository.search(normalizedSearch, normalizedRole, parsedStatus, pageable),
            UserMapper::toResponse
        );
    }

    @Transactional
    public UserResponse updateUser(
        Long userId,
        AdminUpdateUserRequest request,
        String actorEmail
    ) {
        if (!StringUtils.hasText(request.role()) && !StringUtils.hasText(request.status())) {
            throw new ApplicationException(
                HttpStatus.BAD_REQUEST,
                "NO_CHANGES",
                "Can cung cap vai tro hoac trang thai moi"
            );
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new ApplicationException(
            HttpStatus.NOT_FOUND,
            "USER_NOT_FOUND",
            "Khong tim thay nguoi dung"
        ));

        if (user.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new ApplicationException(
                HttpStatus.CONFLICT,
                "SELF_ACCOUNT_PROTECTED",
                "Khong the thay doi vai tro hoac trang thai cua chinh ban"
            );
        }

        if (StringUtils.hasText(request.role())) {
            Role role = roleRepository.findByNameIgnoreCase(request.role().trim())
                .orElseThrow(() -> new ApplicationException(
                    HttpStatus.NOT_FOUND,
                    "ROLE_NOT_FOUND",
                    "Khong tim thay vai tro"
                ));
            user.setRole(role);
        }
        if (StringUtils.hasText(request.status())) {
            user.setStatus(UserStatus.from(request.status().trim()));
        }
        return UserMapper.toResponse(userRepository.save(user));
    }

    private User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ApplicationException(
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND",
                "Khong tim thay nguoi dung"
            ));
    }

    private String trimToNull(String value) {
        return value.isBlank() ? null : value.trim();
    }
}
