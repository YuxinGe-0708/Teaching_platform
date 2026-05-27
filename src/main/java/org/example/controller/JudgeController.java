package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.Submission;
import org.example.entity.Task;
import org.example.service.JudgeService;
import org.example.service.TaskService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/api/v2/judge")
public class JudgeController {

    private final JudgeService judgeService;
    private final TaskService taskService;

    public JudgeController(JudgeService judgeService, TaskService taskService) {
        this.judgeService = judgeService;
        this.taskService = taskService;
    }

    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submitAndJudge(@RequestBody Map<String, Object> body, HttpSession session) {
        // Check login via reflection (compatible with both model.User and entity.User)
        Object sessionUser = session.getAttribute("currentUser");
        if (sessionUser == null) return ApiResponse.fail(401, "请先登录");

        String code = (String) body.get("code");
        String language = (String) body.getOrDefault("language", "python");

        if (code == null || code.trim().isEmpty()) {
            return ApiResponse.fail("代码不能为空");
        }

        // Get test cases from request body
        @SuppressWarnings("unchecked")
        List<Map<String, String>> testCases = (List<Map<String, String>>) body.get("testCases");
        if (testCases == null || testCases.isEmpty()) {
            Map<String, String> tc = new HashMap<>();
            tc.put("input", "");
            tc.put("expectedOutput", "Hello World");
            testCases = Collections.singletonList(tc);
        }

        // Run the judge
        JudgeService.JudgeResult result = judgeService.judge(code, language, testCases);

        Map<String, Object> data = new HashMap<>();
        data.put("status", result.status);
        data.put("score", result.score);
        data.put("passedCases", result.passedCases);
        data.put("totalCases", result.totalCases);
        data.put("timeUsedMs", result.timeUsedMs);
        data.put("caseResults", result.caseResults);
        data.put("errorMessage", result.errorMessage);

        return ApiResponse.ok("评测完成", data);
    }
}
