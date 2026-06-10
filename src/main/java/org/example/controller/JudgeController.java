package org.example.controller;

import org.example.dto.ApiResponse;
import org.example.entity.Submission;
import org.example.entity.Task;
import org.example.entity.User;
import org.example.mapper.SubmissionMapper;
import org.example.service.JudgeService;
import org.example.service.TaskService;
import org.example.util.TaskMetadataUtils;
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

        Long taskId = parseTaskId(body.get("taskId"));
        if (taskId != null) {
            Task task = taskService.findById(taskId);
            if (task == null || !"programming".equals(task.getType())) {
                return ApiResponse.fail(404, "评测任务不存在");
            }
            String allowedLanguage = TaskMetadataUtils.allowedLanguage(task.getDescription());
            if (!sameLanguage(allowedLanguage, language)) {
                return ApiResponse.fail(400, "该实训仅允许提交 " + displayLanguage(allowedLanguage) + " 代码");
            }
        }

        JudgeService.JudgeResult result = judgeService.judge(code, language, testCases);

        if (taskId != null) {
            Submission submission = taskService.submit(taskId, user.getId(), code);
            submission.setScore(result.score);
            submission.setStatus("graded");
            submission.setJudgeResult(result.status);
            submission.setFeedback(result.errorMessage == null ? diagnosis(result) : result.errorMessage);
            submissionMapper.grade(submission);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("status", result.status);
        data.put("score", result.score);
        data.put("passedCases", result.passedCases);
        data.put("totalCases", result.totalCases);
        data.put("timeUsedMs", result.timeUsedMs);
        data.put("memoryUsedKb", result.memoryUsedKb);
        data.put("diagnosis", diagnosis(result));
        data.put("errorMessage", result.errorMessage);
        data.put("usedLocalJudge", result.usedLocalJudge);

        return ApiResponse.ok("评测完成", data);
    }

    private Long parseTaskId(Object rawTaskId) {
        if (rawTaskId == null) return null;
        if (rawTaskId instanceof Number) return ((Number) rawTaskId).longValue();
        try {
            return Long.valueOf(String.valueOf(rawTaskId));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean sameLanguage(String allowedLanguage, String submittedLanguage) {
        return normalizeLanguage(allowedLanguage).equals(normalizeLanguage(submittedLanguage));
    }

    private String normalizeLanguage(String language) {
        if (language == null) return "python";
        String value = language.trim().toLowerCase();
        if ("py".equals(value)) return "python";
        if ("gcc".equals(value)) return "c";
        return value;
    }

    private String displayLanguage(String language) {
        String value = normalizeLanguage(language);
        if ("python".equals(value)) return "Python";
        if ("java".equals(value)) return "Java";
        if ("c".equals(value)) return "C";
        return value;
    }

    private String diagnosis(JudgeService.JudgeResult result) {
        if (result == null) return "评测未返回结果。";
        if ("AC".equals(result.status)) return "答案通过全部评测。";
        if ("WA".equals(result.status)) return "输出结果与期望不一致，请检查算法逻辑、边界条件和输出格式。";
        if ("TLE".equals(result.status)) return "程序运行超时，请优化时间复杂度或检查是否存在死循环。";
        if ("CE".equals(result.status)) return "代码编译失败，请检查语法、类名、头文件或依赖。";
        if ("RE".equals(result.status)) return "程序运行异常，请检查数组越界、空指针、除零或非法输入处理。";
        if (result.errorMessage != null && !result.errorMessage.trim().isEmpty()) return result.errorMessage;
        return "评测服务异常，请稍后重试或联系教师。";
    }
}
