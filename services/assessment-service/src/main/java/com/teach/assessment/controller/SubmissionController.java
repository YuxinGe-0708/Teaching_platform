package com.teach.assessment.controller;

import com.teach.assessment.dto.ApiResponse;
import com.teach.assessment.entity.Submission;
import com.teach.assessment.entity.Task;
import com.teach.assessment.service.TaskService;
import com.teach.assessment.mapper.SubmissionMapper;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/internal/submissions")
public class SubmissionController {
    private final SubmissionMapper mapper; private final TaskService tasks;
    private final com.teach.assessment.service.LearningServiceClient learning; private final com.teach.assessment.service.UserServiceClient users;
    public SubmissionController(SubmissionMapper mapper, TaskService tasks, com.teach.assessment.service.LearningServiceClient learning, com.teach.assessment.service.UserServiceClient users) { this.mapper=mapper; this.tasks=tasks; this.learning=learning; this.users=users; }
    @GetMapping("/task/{taskId}") public ApiResponse<List<Submission>> byTask(@PathVariable Long taskId){return ApiResponse.ok(mapper.findByTaskId(taskId));}
    @GetMapping("/student/{studentId}") public ApiResponse<List<Submission>> byStudent(@PathVariable Long studentId){return ApiResponse.ok(mapper.findByStudentId(studentId));}
    @GetMapping("/{id}") public ApiResponse<Submission> get(@PathVariable Long id){Submission s=mapper.findById(id);return s==null?ApiResponse.fail(404,"提交不存在"):ApiResponse.ok(s);}
    @PostMapping public ApiResponse<Submission> submit(@RequestParam Long taskId,@RequestParam Long studentId,@RequestParam(required=false) String content,@RequestParam(required=false) String filePath){
        Task t=tasks.findById(taskId); if(t==null)return ApiResponse.fail(404,"任务不存在");
        if(!learning.enrolled(t.getCourseId(),studentId))return ApiResponse.fail(403,"未选课，不能提交");
        Submission s=tasks.submit(taskId,studentId,content==null?"":content); if(filePath!=null){s.setFilePath(filePath);mapper.updateContent(s);} return ApiResponse.ok(s);
    }
    @PutMapping("/{id}") public ApiResponse<Submission> update(@PathVariable Long id,@RequestParam(required=false) String content,@RequestParam(required=false) String filePath){Submission s=mapper.findById(id);if(s==null)return ApiResponse.fail(404,"提交不存在");if(content!=null)s.setContent(content);if(filePath!=null)s.setFilePath(filePath);mapper.updateContent(s);return ApiResponse.ok(s);}
    @PostMapping("/{id}/grade") public ApiResponse<Submission> grade(@PathVariable Long id,@RequestParam Double score,@RequestParam(required=false) String feedback,@RequestParam(required=false) String judgeResult){Submission s=mapper.findById(id);if(s==null)return ApiResponse.fail(404,"提交不存在");s.setScore(score);s.setFeedback(feedback);s.setJudgeResult(judgeResult);s.setStatus("graded");mapper.grade(s);users.notify(s.getStudentId(),"成绩已发布",feedback==null?"你的提交已批改":feedback);users.log(s.getStudentId(),"提交批改","submissionId="+id);return ApiResponse.ok(s);}
}
