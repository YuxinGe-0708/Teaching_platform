package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.User;
import org.example.bff.MicroserviceClient;
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

    private final MicroserviceClient microservices;

    public AiController(MicroserviceClient microservices) {
        this.microservices = microservices;
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
        Map<String,String> request = new HashMap<>(body);
        request.put("sessionId", sessionId);
        request.put("courseName", courseName);
        Map<?,?> remote = microservices.post(microservices.learning("/api/v2/ai/chat"), request, Map.class);
        Object remoteReply = remote == null ? null : remote.get("reply");
        String reply = String.valueOf(remoteReply == null ? "" : remoteReply);
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
        microservices.post(microservices.learning("/api/v2/ai/clear"), java.util.Collections.singletonMap("sessionId", sessionId), String.class);
        return ApiResponse.ok("会话已清除");
    }

    @PostMapping("/explain-image")
    public ApiResponse<Map<String, String>> explainImage(@RequestBody Map<String, String> body, HttpSession session) {
        User user = UserController.requireUser(session);
        if (user == null) {
            return ApiResponse.fail(401, "请先登录");
        }
        String image = body.get("image");
        if (image == null || image.trim().isEmpty()) {
            return ApiResponse.fail("框选图片不能为空");
        }
        Map<String,String> request = new HashMap<>(body);
        Map<?,?> remote = microservices.post(microservices.learning("/api/v2/ai/explain-image"), request, Map.class);
        Object remoteReply = remote == null ? null : remote.get("reply");
        String reply = String.valueOf(remoteReply == null ? "" : remoteReply);
        Map<String, String> data = new HashMap<>();
        data.put("reply", reply);
        return ApiResponse.ok(data);
    }
}
