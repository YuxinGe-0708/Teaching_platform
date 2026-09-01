package com.teach.user.controller;

import com.teach.user.dto.ApiResponse;
import com.teach.user.entity.Notification;
import com.teach.user.security.IdentityContext;
import com.teach.user.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<Notification>> list() {
        Long userId = IdentityContext.requireUserId();
        return ApiResponse.ok(notificationService.findByUserId(userId));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount() {
        Long userId = IdentityContext.requireUserId();
        return ApiResponse.ok(notificationService.countUnread(userId));
    }

    @PostMapping("/read")
    public ApiResponse<String> markAsRead(@RequestParam Long notificationId) {
        Long userId = IdentityContext.requireUserId();
        notificationService.markAsRead(notificationId, userId);
        return ApiResponse.ok("ok", null);
    }

    @PostMapping("/read-all")
    public ApiResponse<String> markAllAsRead() {
        Long userId = IdentityContext.requireUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.ok("ok", null);
    }
}
