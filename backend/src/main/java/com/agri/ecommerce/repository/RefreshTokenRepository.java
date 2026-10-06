package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token join fetch token.user where token.tokenHash=:hash")
    Optional<RefreshToken> findByHashForUpdate(@Param("hash") String hash);
    @Modifying @Query("update RefreshToken token set token.revokedAt=current_timestamp where token.user.id=:userId and token.revokedAt is null")
    int revokeAllByUserId(@Param("userId") Long userId);
}
