package com.agri.ecommerce.mapper;

import com.agri.ecommerce.dto.response.RoleResponse;
import com.agri.ecommerce.dto.response.UserResponse;
import com.agri.ecommerce.entity.Permission;
import com.agri.ecommerce.entity.Role;
import com.agri.ecommerce.entity.User;
import java.util.Set;
import java.util.TreeSet;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getStatus().getDatabaseValue(),
            user.getPhoneNumber(),
            user.getAvatar(),
            user.getAddress(),
            user.getRole().getName(),
            permissionNames(user.getRole()),
            user.getLastLoginAt(),
            user.getCreatedAt()
        );
    }

    public static RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getName(), permissionNames(role));
    }

    private static Set<String> permissionNames(Role role) {
        Set<String> names = new TreeSet<>();
        role.getPermissions().stream().map(Permission::getName).forEach(names::add);
        return names;
    }
}
