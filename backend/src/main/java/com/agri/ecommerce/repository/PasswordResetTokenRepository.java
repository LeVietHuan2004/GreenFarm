package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    boolean existsByUser_IdAndCreatedAtAfter(Long userId, LocalDateTime time);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.tokenHash = :hash")
    Optional<PasswordResetToken> findLockedByHash(@Param("hash") String hash);
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
    int invalidateAll(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
