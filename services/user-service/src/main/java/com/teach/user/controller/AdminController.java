package com.teach.user.controller;

import com.teach.user.dto.ApiResponse;
import com.teach.user.dto.UserView;
import com.teach.user.entity.User;
import com.teach.user.security.IdentityContext;
import com.teach.user.service.OperationLogService;
import com.teach.user.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/** 管理员后台：用户管理 + 操作日志。 */
@RestController
@RequestMapping("/api")
public class AdminController {

    private final UserService userService;
    private final OperationLogService operationLogService;

    public AdminController(UserService userService, OperationLogService operationLogService) {
        this.userService = userService;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/users")
    public ApiResponse<List<UserView>> listUsers(@RequestParam(required = false) String role) {
        requireAdmin();
        List<UserView> users = userService.listUsers(role).stream()
                .map(UserView::from)
                .collect(Collectors.toList());
        return ApiResponse.ok(users);
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<String> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        requireAdmin();
        if (!userService.updateStatus(id, status)) return ApiResponse.fail(404, "用户不存在或状态非法");
        return ApiResponse.ok("ok", null);
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<String> resetPassword(@PathVariable Long id, @RequestParam String password) {
        requireAdmin();
        if (!userService.resetPassword(id, password)) return ApiResponse.fail("新密码长度必须为 6-32");
        return ApiResponse.ok("ok", null);
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        requireAdmin();
        if (!userService.deleteUser(id)) return ApiResponse.fail(404, "用户不存在");
        return ApiResponse.ok("ok", null);
    }

    @GetMapping("/logs")
    public ApiResponse<List<Object>> logs() {
        requireAdmin();
        return ApiResponse.ok(operationLogService.findRecent().stream()
                .map(l -> (Object) l)
                .collect(Collectors.toList()));
    }

    private void requireAdmin() {
        if (!"admin".equals(IdentityContext.requireRole())) {
            throw new IllegalStateException("无权访问");
        }
    }
}
