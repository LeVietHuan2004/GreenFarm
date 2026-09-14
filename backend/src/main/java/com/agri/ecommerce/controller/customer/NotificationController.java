package com.agri.ecommerce.controller.customer;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.NotificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public ApiResponse<PageResponse<NotificationResponse>> findAll(@AuthenticationPrincipal GreenFarmUserDetails user, @PageableDefault(size=20) Pageable pageable) { return ApiResponse.success("Lấy thông báo thành công", service.findAll(user.userId(), pageable)); }
    @GetMapping("/unread-count") public ApiResponse<UnreadCountResponse> unreadCount(@AuthenticationPrincipal GreenFarmUserDetails user) { return ApiResponse.success("Lấy số thông báo chưa đọc thành công", service.unreadCount(user.userId())); }
    @PatchMapping("/{id}/read") public ApiResponse<NotificationResponse> markRead(@AuthenticationPrincipal GreenFarmUserDetails user, @PathVariable Long id) { return ApiResponse.success("Đã đọc thông báo", service.markRead(user.userId(), id)); }
    @PatchMapping("/read-all") public ApiResponse<Integer> markAllRead(@AuthenticationPrincipal GreenFarmUserDetails user) { return ApiResponse.success("Đã đọc tất cả thông báo", service.markAllRead(user.userId())); }
}
