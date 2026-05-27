package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.dto.LoginRequest;
import org.example.dto.RegisterRequest;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/auth")
public class ApiAuthController {

    private final UserService userService;
    private final UserMapper userMapper;

    public ApiAuthController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest req) {
        if (req.getUsername() == null || req.getUsername().trim().length() < 3 || req.getUsername().trim().length() > 20) {
            return ApiResponse.fail("Username must be 3-20 characters");
        }
        if (req.getPassword() == null || req.getPassword().trim().length() < 6 || req.getPassword().trim().length() > 32) {
            return ApiResponse.fail("Password must be 6-32 characters");
        }
        if (!"student".equals(req.getRole()) && !"teacher".equals(req.getRole())) {
            return ApiResponse.fail("Invalid role");
        }
        User user = userService.register(req.getUsername().trim(), req.getPassword(), req.getRole(), req.getName());
        if (user == null) {
            return ApiResponse.fail("Username already exists");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        return ApiResponse.ok("Registered", data);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest req, HttpSession session) {
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            return ApiResponse.fail("Please enter username");
        }
        if (req.getPassword() == null || req.getPassword().isEmpty()) {
            return ApiResponse.fail("Please enter password");
        }
        User user = userService.login(req.getUsername().trim(), req.getPassword());
        if (user == null) {
            return ApiResponse.fail("Invalid username or password");
        }
        session.setAttribute("currentUser", user);

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("avatarUrl", user.getAvatarUrl());
        return ApiResponse.ok("OK", data);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> currentUser(HttpSession session) {
        // Handle both model.User (Thymeleaf) and entity.User (REST API)
        Object sessionUser = session.getAttribute("currentUser");
        if (sessionUser == null) {
            return ApiResponse.fail(401, "Not logged in");
        }

        String username = null;
        String role = null;

        // Try to extract username/role from either model.User or entity.User via reflection
        try {
            username = (String) sessionUser.getClass().getMethod("getUsername").invoke(sessionUser);
            role = (String) sessionUser.getClass().getMethod("getRole").invoke(sessionUser);
        } catch (Exception e) {
            return ApiResponse.fail(401, "Invalid session");
        }

        // Look up in MySQL database
        User entityUser = userMapper.findByUsername(username);
        
        Map<String, Object> data = new HashMap<>();
        if (entityUser != null) {
            data.put("id", entityUser.getId());
            data.put("username", entityUser.getUsername());
            data.put("role", entityUser.getRole());
            data.put("name", entityUser.getName());
            data.put("email", entityUser.getEmail());
            data.put("avatarUrl", entityUser.getAvatarUrl());
        } else {
            // User exists in Thymeleaf session but not in MySQL - return basic info
            data.put("id", username.hashCode() & 0x7FFFFFFFL);
            data.put("username", username);
            data.put("role", role);
            data.put("name", username);
            data.put("email", "");
            data.put("avatarUrl", "");
        }
        
        return ApiResponse.ok(data);
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.ok("Logged out");
    }

    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        Object sessionUser = session.getAttribute("currentUser");
        if (sessionUser == null) return ApiResponse.fail(401, "Not logged in");

        String username;
        try {
            username = (String) sessionUser.getClass().getMethod("getUsername").invoke(sessionUser);
        } catch (Exception e) {
            return ApiResponse.fail(401, "Invalid session");
        }

        User entityUser = userMapper.findByUsername(username);
        if (entityUser == null) return ApiResponse.fail("User not found in database");

        if (body.containsKey("name")) entityUser.setName(body.get("name"));
        if (body.containsKey("email")) entityUser.setEmail(body.get("email"));
        if (body.containsKey("avatarUrl")) entityUser.setAvatarUrl(body.get("avatarUrl"));
        userService.updateProfile(entityUser);

        Map<String, Object> data = new HashMap<>();
        data.put("id", entityUser.getId());
        data.put("username", entityUser.getUsername());
        data.put("role", entityUser.getRole());
        data.put("name", entityUser.getName());
        data.put("email", entityUser.getEmail());
        return ApiResponse.ok("Updated", data);
    }
}
