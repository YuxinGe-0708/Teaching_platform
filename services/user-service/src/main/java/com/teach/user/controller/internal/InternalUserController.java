package com.teach.user.controller.internal;

import com.teach.user.dto.ApiResponse;
import com.teach.user.dto.UserView;
import com.teach.user.entity.User;
import com.teach.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务间接口：供 learning/assessment 取用户基础信息与姓名。
 * 仅查询 user 表，不做任何跨域联表。
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ApiResponse<UserView> byId(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) return ApiResponse.fail(404, "用户不存在");
        return ApiResponse.ok(UserView.from(user));
    }

    @GetMapping("/by-ids")
    public ApiResponse<List<UserView>> byIds(@RequestParam("ids") List<Long> ids) {
        List<UserView> result = new ArrayList<>();
        if (ids != null) {
            result = ids.stream()
                    .map(userService::findById)
                    .filter(u -> u != null)
                    .map(UserView::from)
                    .collect(Collectors.toList());
        }
        return ApiResponse.ok(result);
    }

    @GetMapping
    public ApiResponse<List<UserView>> byRole(@RequestParam(value = "role", required = false) String role,
                                              @RequestParam(value = "status", required = false) Integer status) {
        List<UserView> users = userService.listUsers(role).stream()
                .filter(u -> status == null || u.getStatus() == null || u.getStatus().equals(status))
                .map(UserView::from)
                .collect(Collectors.toList());
        return ApiResponse.ok(users);
    }
}
