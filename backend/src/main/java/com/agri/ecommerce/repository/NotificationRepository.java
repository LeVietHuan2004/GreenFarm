package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findAllByUser_IdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
    long countByUser_IdAndReadFalse(Long userId);
    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);
    @Modifying @Query("update Notification notification set notification.read = true where notification.user.id = :userId and notification.read = false")
    int markAllRead(@Param("userId") Long userId);
}
