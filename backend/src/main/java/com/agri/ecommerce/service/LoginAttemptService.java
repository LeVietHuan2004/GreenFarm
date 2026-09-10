package com.agri.ecommerce.service;

import com.agri.ecommerce.entity.User;
import com.agri.ecommerce.repository.UserRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    private final UserRepository userRepository;
    private final int maxAttempts;
    private final long lockDurationMinutes;

    public LoginAttemptService(
        UserRepository userRepository,
        @Value("${app.security.login.max-attempts}") int maxAttempts,
        @Value("${app.security.login.lock-duration-minutes}") long lockDurationMinutes
    ) {
        this.userRepository = userRepository;
        this.maxAttempts = maxAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoginAttemptResult recordFailure(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts >= maxAttempts) {
            LocalDateTime lockedUntil = now.plusMinutes(lockDurationMinutes);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(lockedUntil);
            userRepository.save(user);
            return new LoginAttemptResult(true, 0, lockedUntil);
        }

        user.setFailedLoginAttempts(attempts);
        userRepository.save(user);
        return new LoginAttemptResult(false, maxAttempts - attempts, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User recordSuccess(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public record LoginAttemptResult(
        boolean locked,
        int remainingAttempts,
        LocalDateTime lockedUntil
    ) {
    }
}
