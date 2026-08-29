package com.teach.user.controller;

import com.teach.user.dto.ApiResponse;
import com.teach.user.dto.UserView;
import com.teach.user.entity.User;
import com.teach.user.security.IdentityContext;
import com.teach.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<UserView> profile() {
        Long userId = IdentityContext.requireUserId();
        User user = userService.findById(userId);
        if (user == null) return ApiResponse.fail(404, "用户不存在");
        return ApiResponse.ok(UserView.from(user));
    }

    @PutMapping
    public ApiResponse<UserView> update(@RequestParam(required = false) String name,
                                        @RequestParam(required = false) String email) {
        Long userId = IdentityContext.requireUserId();
        User user = userService.findById(userId);
        if (user == null) return ApiResponse.fail(404, "用户不存在");
        if (name != null) user.setName(name.trim().isEmpty() ? user.getUsername() : name.trim());
        if (email != null) user.setEmail(email.trim());
        userService.updateProfile(user);
        return ApiResponse.ok(UserView.from(userService.findById(userId)));
    }
}
