package com.teach.user.controller;

import com.teach.user.dto.ApiResponse;
import com.teach.user.dto.LoginRequest;
import com.teach.user.dto.LoginResponse;
import com.teach.user.dto.RegisterRequest;
import com.teach.user.dto.UserView;
import com.teach.user.entity.User;
import com.teach.user.security.JwtUtil;
import com.teach.user.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        User user = userService.login(req.getUsername(), req.getPassword());
        if (user == null) {
            return ApiResponse.fail(401, "用户名或密码错误，或账号已禁用");
        }
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return ApiResponse.ok(new LoginResponse(token, UserView.from(user)));
    }

    @PostMapping("/register")
    public ApiResponse<UserView> register(@RequestBody @Valid RegisterRequest req) {
        User user = userService.register(req.getUsername(), req.getPassword(), req.getRole(), req.getName());
        if (user == null) {
            return ApiResponse.fail("用户名已存在");
        }
        return ApiResponse.ok("注册成功", UserView.from(user));
    }
}
