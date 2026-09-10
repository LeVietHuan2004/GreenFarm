package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.LoginRequest;
import com.agri.ecommerce.dto.request.RegisterRequest;
import com.agri.ecommerce.dto.response.AuthResponse;
import com.agri.ecommerce.entity.Role;
import com.agri.ecommerce.entity.User;
import com.agri.ecommerce.entity.UserStatus;
import com.agri.ecommerce.repository.RoleRepository;
import com.agri.ecommerce.repository.UserRepository;
import com.agri.ecommerce.security.JwtService;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private Role customerRole;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            roleRepository,
            passwordEncoder,
            jwtService,
            loginAttemptService
        );
    }

    @Test
    void registersActiveCustomerWithEncodedPassword() {
        when(userRepository.existsByEmailIgnoreCase("new@greenfarm.local")).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase("customer"))
            .thenReturn(Optional.of(customerRole));
        when(customerRole.getName()).thenReturn("customer");
        when(customerRole.getPermissions()).thenReturn(Set.of());
        when(passwordEncoder.encode("GreenFarm!2026")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("signed-jwt");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.register(new RegisterRequest(
            "New Customer",
            " NEW@greenfarm.local ",
            "GreenFarm!2026",
            "0901234567"
        ));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("new@greenfarm.local");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getRole()).isSameAs(customerRole);
        assertThat(response.accessToken()).isEqualTo("signed-jwt");
        assertThat(response.user().role()).isEqualTo("customer");
    }

    @Test
    void recordsFailureAndReturnsRemainingAttemptsForWrongPassword() {
        User user = org.mockito.Mockito.mock(User.class);
        when(userRepository.findByEmailIgnoreCase("user@greenfarm.local"))
            .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(7L);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getPassword()).thenReturn("encoded-password");
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);
        when(loginAttemptService.recordFailure(7L))
            .thenReturn(new LoginAttemptService.LoginAttemptResult(false, 4, null));

        assertThatThrownBy(() -> authService.login(new LoginRequest(
            "user@greenfarm.local",
            "wrong-password",
            null
        )))
            .isInstanceOf(ApplicationException.class)
            .extracting(exception -> ((ApplicationException) exception).getCode())
            .isEqualTo("INVALID_CREDENTIALS");

        verify(loginAttemptService).recordFailure(7L);
    }

    @Test
    void rejectsLoginWhenAccountRoleDoesNotMatchSelectedPortal() {
        User user = org.mockito.Mockito.mock(User.class);
        when(userRepository.findByEmailIgnoreCase("user@greenfarm.local"))
            .thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getPassword()).thenReturn("encoded-password");
        when(user.getRole()).thenReturn(customerRole);
        when(customerRole.getName()).thenReturn("customer");
        when(passwordEncoder.matches("correct-password", "encoded-password"))
            .thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest(
            "user@greenfarm.local",
            "correct-password",
            "admin"
        )))
            .isInstanceOf(ApplicationException.class)
            .extracting(exception -> ((ApplicationException) exception).getCode())
            .isEqualTo("ROLE_MISMATCH");

        verify(loginAttemptService, never()).recordSuccess(anyLong());
    }
}
