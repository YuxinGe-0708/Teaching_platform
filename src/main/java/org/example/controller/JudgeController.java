package org.example.controller;

import org.example.bff.MicroserviceClient;
import org.example.dto.ApiResponse;
import org.example.entity.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/judge")
public class JudgeController {
    private final MicroserviceClient microservices;

    public JudgeController(MicroserviceClient microservices) {
        this.microservices = microservices;
    }

    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submitAndJudge(@RequestBody Map<String, Object> body,
                                                            HttpSession session) {
        User user = UserController.requireUser(session);
        if (user == null) return ApiResponse.fail(401, "请先登录");
        if (!"student".equals(user.getRole())) return ApiResponse.fail(403, "仅学生可以提交评测");

        Map<String, Object> request = new LinkedHashMap<>(body);
        request.put("studentId", user.getId());
        Map<?, ?> remote = microservices.post(
                microservices.assessment("/api/v2/judge/submit"), request, Map.class);
        Map<String, Object> result = new LinkedHashMap<>();
        if (remote != null) {
            for (Map.Entry<?, ?> entry : remote.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return ApiResponse.ok("评测完成", result);
    }
}
