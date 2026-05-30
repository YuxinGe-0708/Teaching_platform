package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.User;
import org.example.service.AiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
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
        User user = UserController.requireUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "请先登录");
        }
        String message = body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ApiResponse.fail("消息不能为空");
        }
        String courseName = body.getOrDefault("courseName", "通用课程");
        String sessionId = "user_" + user.getId() + "_course_" + body.getOrDefault("courseId", "0");
        String reply = aiService.chat(sessionId, courseName, message.trim());
        Map<String, String> data = new HashMap<>();
        data.put("reply", reply);
        return ApiResponse.ok(data);
    }

    @PostMapping("/clear")
    public ApiResponse<String> clearSession(@RequestBody Map<String, String> body, HttpSession session) {
        User user = UserController.requireUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "请先登录");
        }
        String sessionId = "user_" + user.getId() + "_course_" + body.getOrDefault("courseId", "0");
        aiService.clearSession(sessionId);
        return ApiResponse.ok("会话已清除");
    }
}
