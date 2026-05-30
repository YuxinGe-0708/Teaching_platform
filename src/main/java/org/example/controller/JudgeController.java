package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.Submission;
import org.example.entity.User;
import org.example.mapper.SubmissionMapper;
import org.example.service.JudgeService;
import org.example.service.TaskService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/judge")
public class JudgeController {

    private final JudgeService judgeService;
    private final TaskService taskService;
    private final SubmissionMapper submissionMapper;

    public JudgeController(JudgeService judgeService, TaskService taskService, SubmissionMapper submissionMapper) {
        this.judgeService = judgeService;
        this.taskService = taskService;
        this.submissionMapper = submissionMapper;
    }

    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submitAndJudge(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = UserController.requireUser(session);
        if (user == null) return ApiResponse.fail(401, "请先登录");

        String code = (String) body.get("code");
        String language = (String) body.getOrDefault("language", "python");
        if (code == null || code.trim().isEmpty()) {
            return ApiResponse.fail("代码不能为空");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> testCases = (List<Map<String, String>>) body.get("testCases");
        if (testCases == null || testCases.isEmpty()) {
            Map<String, String> tc = new HashMap<>();
            tc.put("input", "");
            tc.put("expectedOutput", "Hello World");
            testCases = Collections.singletonList(tc);
        }

        JudgeService.JudgeResult result = judgeService.judge(code, language, testCases);

        Object rawTaskId = body.get("taskId");
        if (rawTaskId != null) {
            Long taskId = rawTaskId instanceof Number
                    ? ((Number) rawTaskId).longValue()
                    : Long.valueOf(String.valueOf(rawTaskId));
            Submission submission = taskService.submit(taskId, user.getId(), code);
            submission.setScore(result.score);
            submission.setStatus("graded");
            submission.setJudgeResult(result.status);
            submission.setFeedback(result.errorMessage);
            submissionMapper.grade(submission);
        }

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
