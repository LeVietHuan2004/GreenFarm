package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.agri.ecommerce.entity.User;
import com.agri.ecommerce.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private UserRepository userRepository;

    private LoginAttemptService loginAttemptService;
    private User user;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService(userRepository, 5, 15);
        user = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void locksAccountOnFifthFailedAttempt() {
        for (int attempt = 1; attempt < 5; attempt++) {
            LoginAttemptService.LoginAttemptResult result =
                loginAttemptService.recordFailure(1L);
            assertThat(result.locked()).isFalse();
            assertThat(result.remainingAttempts()).isEqualTo(5 - attempt);
        }

        LocalDateTime beforeLock = LocalDateTime.now();
        LoginAttemptService.LoginAttemptResult result = loginAttemptService.recordFailure(1L);

        assertThat(result.locked()).isTrue();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isAfter(beforeLock.plusMinutes(14));
    }

    @Test
    void successfulLoginClearsFailuresAndRecordsTimestamp() {
        user.setFailedLoginAttempts(3);
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));

        User result = loginAttemptService.recordSuccess(1L);

        assertThat(result.getFailedLoginAttempts()).isZero();
        assertThat(result.getLockedUntil()).isNull();
        assertThat(result.getLastLoginAt()).isNotNull();
    }
}
