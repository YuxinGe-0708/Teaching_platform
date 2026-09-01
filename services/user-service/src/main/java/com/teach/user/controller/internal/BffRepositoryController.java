package com.teach.user.controller.internal;

import com.teach.user.dto.ApiResponse;
import com.teach.user.entity.Notification;
import com.teach.user.entity.OperationLog;
import com.teach.user.entity.User;
import com.teach.user.mapper.NotificationMapper;
import com.teach.user.mapper.OperationLogMapper;
import com.teach.user.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Internal persistence facade used only by the server-side page BFF. */
@RestController
@RequestMapping("/internal/bff")
public class BffRepositoryController {
    private final UserMapper users;
    private final NotificationMapper notifications;
    private final OperationLogMapper logs;

    public BffRepositoryController(UserMapper users, NotificationMapper notifications, OperationLogMapper logs) {
        this.users = users;
        this.notifications = notifications;
        this.logs = logs;
    }

    @GetMapping("/users/{id}") public ApiResponse<User> user(@PathVariable Long id) { return ApiResponse.ok(users.findById(id)); }
    @GetMapping("/users/by-username") public ApiResponse<User> userByName(@RequestParam String username) { return ApiResponse.ok(users.findByUsername(username)); }
    @GetMapping("/users") public ApiResponse<List<User>> userList(@RequestParam(required=false) String role) { return ApiResponse.ok(role == null || role.isEmpty() ? users.findAll() : users.findByRole(role)); }
    @GetMapping("/users/count") public ApiResponse<Integer> userCount(@RequestParam(required=false) String role) { return ApiResponse.ok(role == null || role.isEmpty() ? users.countAll() : users.countByRole(role)); }
    @PostMapping("/users") public ApiResponse<User> insertUser(@RequestBody User user) { users.insert(user); return ApiResponse.ok(user); }
    @PutMapping("/users/{id}/profile") public ApiResponse<Integer> updateUser(@PathVariable Long id, @RequestBody User user) { user.setId(id); return ApiResponse.ok(users.updateProfile(user)); }
    @PutMapping("/users/{id}/admin") public ApiResponse<Integer> updateUserAdmin(@PathVariable Long id, @RequestBody User user) { user.setId(id); return ApiResponse.ok(users.updateByAdmin(user)); }
    @PutMapping("/users/{id}/password") public ApiResponse<Integer> updatePassword(@PathVariable Long id, @RequestParam String password) { return ApiResponse.ok(users.updatePassword(id, password)); }
    @DeleteMapping("/users/{id}") public ApiResponse<Integer> deleteUser(@PathVariable Long id) { return ApiResponse.ok(users.deleteById(id)); }

    @GetMapping("/notifications/user/{userId}") public ApiResponse<List<Notification>> notifications(@PathVariable Long userId) { return ApiResponse.ok(notifications.findByUserId(userId)); }
    @GetMapping("/notifications/{id}") public ApiResponse<Notification> notification(@PathVariable Long id) { return ApiResponse.ok(notifications.findById(id)); }
    @GetMapping("/notifications/recent") public ApiResponse<List<Notification>> recentNotifications() { return ApiResponse.ok(notifications.findRecent()); }
    @GetMapping("/notifications/unread-count/{userId}") public ApiResponse<Integer> unread(@PathVariable Long userId) { return ApiResponse.ok(notifications.countUnread(userId)); }
    @PostMapping("/notifications") public ApiResponse<Notification> insertNotification(@RequestBody Notification value) { notifications.insert(value); return ApiResponse.ok(value); }
    @PutMapping("/notifications/{id}/read") public ApiResponse<Integer> read(@PathVariable Long id, @RequestParam(required=false) Long userId) { return ApiResponse.ok(userId == null ? notifications.markAsRead(id) : notifications.markAsReadForUser(id, userId)); }
    @PutMapping("/notifications/read-all/{userId}") public ApiResponse<Integer> readAll(@PathVariable Long userId) { return ApiResponse.ok(notifications.markAllAsRead(userId)); }
    @DeleteMapping("/notifications/{id}") public ApiResponse<Integer> deleteNotification(@PathVariable Long id) { return ApiResponse.ok(notifications.deleteById(id)); }

    @PostMapping("/logs") public ApiResponse<OperationLog> insertLog(@RequestBody OperationLog value) { logs.insert(value); return ApiResponse.ok(value); }
    @GetMapping("/logs/user/{userId}") public ApiResponse<List<OperationLog>> userLogs(@PathVariable Long userId) { return ApiResponse.ok(logs.findByUserId(userId)); }
    @GetMapping("/logs/recent") public ApiResponse<List<OperationLog>> recentLogs() { return ApiResponse.ok(logs.findRecent()); }
}
