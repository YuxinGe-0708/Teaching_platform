package com.teach.assessment.controller;

import com.teach.assessment.dto.ApiResponse;
import com.teach.assessment.entity.ExamRecord;
import com.teach.assessment.entity.Task;
import com.teach.assessment.mapper.ExamRecordMapper;
import com.teach.assessment.service.ExamService;
import com.teach.assessment.service.TaskService;
import com.teach.assessment.service.LearningServiceClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/exams")
public class ExamController {
    private final ExamService exams; private final TaskService tasks; private final ExamRecordMapper mapper; private final LearningServiceClient learning;
    public ExamController(ExamService exams,TaskService tasks,ExamRecordMapper mapper,LearningServiceClient learning){this.exams=exams;this.tasks=tasks;this.mapper=mapper;this.learning=learning;}
    @GetMapping("/{taskId}/student/{studentId}") public ApiResponse<ExamRecord> get(@PathVariable Long taskId,@PathVariable Long studentId){return ApiResponse.ok(exams.getExamRecord(studentId,taskId));}
    @PostMapping("/{taskId}/begin") public ApiResponse<ExamRecord> begin(@PathVariable Long taskId,@RequestParam Long studentId){Task t=tasks.findById(taskId);if(t==null||!"exam".equals(t.getType()))return ApiResponse.fail(404,"考试不存在");if(!learning.enrolled(t.getCourseId(),studentId))return ApiResponse.fail(403,"未选课，不能参加考试");return ApiResponse.ok(exams.beginExam(studentId,taskId));}
    @PutMapping("/{taskId}/progress") public ApiResponse<ExamRecord> progress(@PathVariable Long taskId,@RequestParam Long studentId,@RequestParam(required=false) String content){Task t=tasks.findById(taskId);if(t==null||!learning.enrolled(t.getCourseId(),studentId))return ApiResponse.fail(403,"无权暂存该考试");return ApiResponse.ok(exams.saveProgress(studentId,taskId,content));}
    @PostMapping("/{taskId}/submit") public ApiResponse<ExamRecord> submit(@PathVariable Long taskId,@RequestParam Long studentId,@RequestParam(required=false) String content,@RequestParam(required=false,defaultValue="false") boolean auto){Task t=tasks.findById(taskId);if(t==null||!learning.enrolled(t.getCourseId(),studentId))return ApiResponse.fail(403,"无权提交该考试");ExamRecord r=auto?exams.autoSubmitExam(studentId,taskId,content):exams.submitExam(studentId,taskId,content);exams.createSubmissionFromExam(r,t);return ApiResponse.ok(r);}
}
