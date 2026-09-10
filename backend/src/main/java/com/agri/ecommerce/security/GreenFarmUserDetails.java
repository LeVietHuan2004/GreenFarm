package com.agri.ecommerce.security;

import com.agri.ecommerce.entity.Permission;
import com.agri.ecommerce.entity.User;
import com.agri.ecommerce.entity.UserStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record GreenFarmUserDetails(
    Long userId,
    String username,
    String password,
    Collection<? extends GrantedAuthority> authorities,
    boolean enabled,
    boolean accountNonLocked
) implements UserDetails {

    public static GreenFarmUserDetails from(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(
            "ROLE_" + user.getRole().getName().toUpperCase(Locale.ROOT)
        ));
        user.getRole().getPermissions().stream()
            .map(Permission::getName)
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);

        return new GreenFarmUserDetails(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            List.copyOf(authorities),
            user.getStatus() == UserStatus.ACTIVE,
            !user.isTemporarilyLocked(LocalDateTime.now())
        );
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
