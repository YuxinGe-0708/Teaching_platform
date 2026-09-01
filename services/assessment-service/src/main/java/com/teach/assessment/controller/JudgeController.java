package com.teach.assessment.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teach.assessment.dto.ApiResponse;
import com.teach.assessment.entity.Submission;
import com.teach.assessment.entity.Task;
import com.teach.assessment.mapper.SubmissionMapper;
import com.teach.assessment.service.JudgeService;
import com.teach.assessment.service.TaskService;
import com.teach.assessment.util.TaskMetadataUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/judge")
public class JudgeController {
    private final JudgeService judge;
    private final TaskService tasks;
    private final SubmissionMapper submissions;
    private final ObjectMapper mapper = new ObjectMapper();

    public JudgeController(JudgeService judge, TaskService tasks, SubmissionMapper submissions) {
        this.judge = judge;
        this.tasks = tasks;
        this.submissions = submissions;
    }

    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.getOrDefault("code", ""));
        if (code.trim().isEmpty()) return ApiResponse.fail("代码不能为空");

        String language = String.valueOf(body.getOrDefault("language", "python"));
        Long taskId = number(body.get("taskId"));
        Long studentId = number(body.get("studentId"));
        List<Map<String, String>> cases = new ArrayList<>();
        if (taskId != null) {
            Task task = tasks.findById(taskId);
            if (task == null || !"programming".equals(task.getType())) {
                return ApiResponse.fail(404, "评测任务不存在");
            }
            String allowed = TaskMetadataUtils.allowedLanguage(task.getDescription());
            if (!"any".equals(allowed) && !normalize(allowed).equals(normalize(language))) {
                return ApiResponse.fail(400, "该实训不允许使用此语言");
            }
            try {
                cases = mapper.readValue(TaskMetadataUtils.testCasesJson(task.getDescription()),
                        new TypeReference<List<Map<String, String>>>() { });
            } catch (Exception ignored) {
                // The judge returns a stable configuration error when no valid case remains.
            }
        }
        if (cases.isEmpty()) {
            Map<String, String> fallback = new HashMap<>();
            fallback.put("input", "");
            fallback.put("expectedOutput", "Hello World");
            cases.add(fallback);
        }

        JudgeService.JudgeResult result = judge.judge(code, language, cases);
        if (taskId != null && studentId != null) {
            Submission submission = tasks.submit(taskId, studentId, code);
            submission.setScore(result.score);
            submission.setStatus("graded");
            submission.setJudgeResult(result.status);
            submission.setFeedback(result.errorMessage == null ? diagnosis(result) : result.errorMessage);
            submissions.grade(submission);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", result.status);
        response.put("score", result.score);
        response.put("passedCases", result.passedCases);
        response.put("totalCases", result.totalCases);
        response.put("timeUsedMs", result.timeUsedMs);
        response.put("memoryUsedKb", result.memoryUsedKb);
        response.put("diagnosis", diagnosis(result));
        response.put("errorMessage", result.errorMessage);
        response.put("usedLocalJudge", result.usedLocalJudge);
        return ApiResponse.ok("评测完成", response);
    }

    private Long number(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String normalize(String value) {
        String normalized = value == null ? "python" : value.trim().toLowerCase();
        if ("py".equals(normalized)) return "python";
        if ("gcc".equals(normalized)) return "c";
        return normalized;
    }

    private String diagnosis(JudgeService.JudgeResult result) {
        if (result == null) return "评测未返回结果。";
        if ("AC".equals(result.status)) return "答案通过全部评测。";
        if ("WA".equals(result.status)) return "输出结果与期望不一致，请检查算法逻辑、边界条件和输出格式。";
        if ("TLE".equals(result.status)) return "程序运行超时，请优化时间复杂度或检查是否存在死循环。";
        if ("CE".equals(result.status)) return "代码编译失败，请检查语法、类名、头文件或依赖。";
        if ("RE".equals(result.status)) return "程序运行异常，请检查数组越界、空指针、除零或非法输入处理。";
        return result.errorMessage == null ? "评测服务异常，请稍后重试。" : result.errorMessage;
    }
}
