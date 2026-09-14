package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional
    public void notifyUser(User user, String type, String message, String link) {
        if (user == null || user.getId() == null) return;
        Notification notification = new Notification();
        notification.setUser(user); notification.setType(type); notification.setMessage(message); notification.setLink(link);
        notifications.save(notification);
    }

    @Transactional
    public void notifyRole(String role, String type, String message, String link) {
        users.findAllByRoleAndStatus(role, UserStatus.ACTIVE)
            .forEach(user -> notifyUser(user, type, message, link));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> findAll(Long userId, Pageable pageable) {
        return PageResponse.from(notifications.findAllByUser_IdOrderByCreatedAtDescIdDesc(userId, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notifications.countByUser_IdAndReadFalse(userId));
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long id) {
        Notification notification = notifications.findByIdAndUser_Id(id, userId).orElseThrow(() ->
            new ApplicationException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo"));
        notification.setRead(true);
        return toResponse(notifications.save(notification));
    }

    @Transactional
    public int markAllRead(Long userId) { return notifications.markAllRead(userId); }

    private NotificationResponse toResponse(Notification value) {
        return new NotificationResponse(value.getId(), value.getType(), value.getMessage(), value.getLink(), value.isRead(), value.getCreatedAt());
    }
}
