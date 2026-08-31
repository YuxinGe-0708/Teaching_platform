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
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v2/judge")
public class JudgeController {
    private final JudgeService judge; private final TaskService tasks; private final SubmissionMapper submissions; private final ObjectMapper mapper=new ObjectMapper();
    public JudgeController(JudgeService judge,TaskService tasks,SubmissionMapper submissions){this.judge=judge;this.tasks=tasks;this.submissions=submissions;}
    @PostMapping("/submit") public ApiResponse<Map<String,Object>> submit(@RequestBody Map<String,Object> body){String code=String.valueOf(body.getOrDefault("code",""));if(code.trim().isEmpty())return ApiResponse.fail("代码不能为空");String language=String.valueOf(body.getOrDefault("language","python"));Long taskId=body.get("taskId") instanceof Number?((Number)body.get("taskId")).longValue():null;List<Map<String,String>> cases=new ArrayList<>();if(taskId!=null){Task t=tasks.findById(taskId);if(t==null||!"programming".equals(t.getType()))return ApiResponse.fail(404,"评测任务不存在");try{cases=mapper.readValue(TaskMetadataUtils.testCasesJson(t.getDescription()),new TypeReference<List<Map<String,String>>>(){});}catch(Exception ignored){}}if(cases.isEmpty()){Map<String,String> c=new HashMap<>();c.put("input","");c.put("expectedOutput","Hello World");cases.add(c);}JudgeService.JudgeResult r=judge.judge(code,language,cases);if(taskId!=null&&body.get("studentId") instanceof Number){Submission s=tasks.submit(taskId,((Number)body.get("studentId")).longValue(),code);s.setScore(r.score);s.setStatus("graded");s.setJudgeResult(r.status);s.setFeedback(r.errorMessage);submissions.grade(s);}Map<String,Object> out=new LinkedHashMap<>();out.put("status",r.status);out.put("score",r.score);out.put("passedCases",r.passedCases);out.put("totalCases",r.totalCases);out.put("timeUsedMs",r.timeUsedMs);out.put("memoryUsedKb",r.memoryUsedKb);out.put("errorMessage",r.errorMessage);return ApiResponse.ok("评测完成",out);}
}
