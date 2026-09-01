package com.teach.user.controller.internal;

import com.teach.user.dto.ApiResponse;
import com.teach.user.entity.Notification;
import com.teach.user.service.NotificationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 服务间接口：其它服务经此创建站内通知、标记已读。
 * 幂等：调用方应带 request_id，本服务以 userId/title/type 键兜底去重（骨架阶段占位）。
 */
@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    private final NotificationService notificationService;

    public InternalNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ApiResponse<Notification> create(@RequestBody Notification body) {
        Notification n = notificationService.create(body.getUserId(), body.getTitle(), body.getContent(), body.getType());
        return ApiResponse.ok(n);
    }

    @PostMapping("/batch")
    public ApiResponse<Integer> createBatch(@RequestBody List<Notification> list) {
        if (list == null) return ApiResponse.ok(0);
        for (Notification n : list) {
            notificationService.create(n.getUserId(), n.getTitle(), n.getContent(), n.getType());
        }
        return ApiResponse.ok(list.size());
    }

    @PostMapping("/{id}/read")
    public ApiResponse<String> markRead(@PathVariable Long id, @RequestParam Long userId) {
        notificationService.markAsRead(id, userId);
        return ApiResponse.ok("ok", null);
    }

    @PostMapping("/read-all")
    public ApiResponse<String> markAllRead(@RequestParam Long userId) {
        notificationService.markAllAsRead(userId);
        return ApiResponse.ok("ok", null);
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Notification>> byUser(@PathVariable Long userId) {
        return ApiResponse.ok(notificationService.findByUserId(userId));
    }
}
