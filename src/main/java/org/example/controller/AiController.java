package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.service.AiService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public ApiResponse<Map<String, String>> chat(@RequestBody Map<String, String> body, HttpSession session) {
        Object sessionUser = session.getAttribute("currentUser");
        if (sessionUser == null) {
            return ApiResponse.fail(401, "请先登录");
        }

        String message = body.get("message");
        String courseName = body.getOrDefault("courseName", "通用课程");

        if (message == null || message.trim().isEmpty()) {
            return ApiResponse.fail("消息不能为空");
        }

        String username;
        try {
            username = (String) sessionUser.getClass().getMethod("getUsername").invoke(sessionUser);
        } catch (Exception e) {
            return ApiResponse.fail(401, "无效的会话");
        }

        String sessionId = "user_" + username + "_course_" + body.getOrDefault("courseId", "0");
        String reply = aiService.chat(sessionId, courseName, message.trim());

        Map<String, String> data = new java.util.HashMap<>();
        data.put("reply", reply);
        return ApiResponse.ok(data);
    }

    @PostMapping("/clear")
    public ApiResponse<String> clearSession(@RequestBody Map<String, String> body, HttpSession session) {
        Object sessionUser = session.getAttribute("currentUser");
        if (sessionUser == null) {
            return ApiResponse.fail(401, "请先登录");
        }
        String username;
        try {
            username = (String) sessionUser.getClass().getMethod("getUsername").invoke(sessionUser);
        } catch (Exception e) {
            return ApiResponse.fail(401, "无效的会话");
        }
        String sessionId = "user_" + username + "_course_" + body.getOrDefault("courseId", "0");
        aiService.clearSession(sessionId);
        return ApiResponse.ok("会话已清除");
    }
}
