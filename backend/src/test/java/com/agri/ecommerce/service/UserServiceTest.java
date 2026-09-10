package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.AdminUpdateUserRequest;
import com.agri.ecommerce.entity.User;
import com.agri.ecommerce.entity.UserStatus;
import com.agri.ecommerce.repository.RoleRepository;
import com.agri.ecommerce.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void normalizesAdminUserFilters() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(userRepository.search("Nguyen", "admin", UserStatus.ACTIVE, pageable))
            .thenReturn(Page.empty(pageable));

        var response = userService.findUsers("  Nguyen  ", " admin ", " active ", pageable);

        assertThat(response.totalElements()).isZero();
        verify(userRepository).search("Nguyen", "admin", UserStatus.ACTIVE, pageable);
    }

    @Test
    void preventsAdministratorFromChangingOwnAccess() {
        User admin = new User();
        admin.setEmail("admin@greenfarm.local");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.updateUser(
            1L,
            new AdminUpdateUserRequest("staff", null),
            "admin@greenfarm.local"
        ))
            .isInstanceOf(ApplicationException.class)
            .extracting("code")
            .isEqualTo("SELF_ACCOUNT_PROTECTED");
    }
}
