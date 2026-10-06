package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.GuestSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface GuestSessionRepository extends JpaRepository<GuestSession,Long> {
    Optional<GuestSession> findByTokenHash(String tokenHash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from GuestSession session where session.tokenHash=:tokenHash")
    Optional<GuestSession> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
