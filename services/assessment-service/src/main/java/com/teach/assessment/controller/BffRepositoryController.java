package com.teach.assessment.controller;

import com.teach.assessment.dto.ApiResponse;
import com.teach.assessment.entity.ExamRecord;
import com.teach.assessment.entity.Submission;
import com.teach.assessment.entity.Task;
import com.teach.assessment.mapper.ExamRecordMapper;
import com.teach.assessment.mapper.SubmissionMapper;
import com.teach.assessment.mapper.TaskMapper;
import com.teach.assessment.service.LearningServiceClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Internal persistence facade used only by the Thymeleaf BFF. */
@RestController
@RequestMapping("/internal/bff")
public class BffRepositoryController {
    private final TaskMapper tasks; private final SubmissionMapper submissions; private final ExamRecordMapper exams; private final LearningServiceClient learning;
    public BffRepositoryController(TaskMapper tasks,SubmissionMapper submissions,ExamRecordMapper exams,LearningServiceClient learning){this.tasks=tasks;this.submissions=submissions;this.exams=exams;this.learning=learning;}

    @GetMapping("/tasks/{id}") public ApiResponse<Task> task(@PathVariable Long id){return ApiResponse.ok(tasks.findById(id));}
    @GetMapping("/tasks") public ApiResponse<List<Task>> taskList(@RequestParam(required=false) Long courseId,@RequestParam(required=false) Long studentId){
        if(courseId!=null)return ApiResponse.ok(tasks.findByCourseId(courseId));
        if(studentId!=null){java.util.Set<Long> enrolled=learning.enrolledCourseIds(studentId);java.util.List<Task> result=new java.util.ArrayList<>();for(Task t:tasks.findPublished())if(enrolled.contains(t.getCourseId()))result.add(t);return ApiResponse.ok(result);}
        return ApiResponse.ok(java.util.Collections.<Task>emptyList());
    }
    @GetMapping("/tasks/count") public ApiResponse<Integer> taskCount(){return ApiResponse.ok(tasks.countAll());}
    @PostMapping("/tasks") public ApiResponse<Task> insertTask(@RequestBody Task value){tasks.insert(value);return ApiResponse.ok(value);}
    @PutMapping("/tasks/{id}") public ApiResponse<Integer> updateTask(@PathVariable Long id,@RequestBody Task value){value.setId(id);return ApiResponse.ok(tasks.update(value));}
    @PutMapping("/tasks/{id}/status") public ApiResponse<Integer> taskStatus(@PathVariable Long id,@RequestParam String status){return ApiResponse.ok(tasks.updateStatus(id,status));}
    @DeleteMapping("/tasks/{id}") public ApiResponse<Integer> deleteTask(@PathVariable Long id){return ApiResponse.ok(tasks.delete(id));}

    @GetMapping("/submissions/{id}") public ApiResponse<Submission> submission(@PathVariable Long id){return ApiResponse.ok(submissions.findById(id));}
    @GetMapping("/submissions/task/{id}") public ApiResponse<List<Submission>> taskSubmissions(@PathVariable Long id){return ApiResponse.ok(submissions.findByTaskId(id));}
    @GetMapping("/submissions/student-task") public ApiResponse<Submission> studentTaskSubmission(@RequestParam Long studentId,@RequestParam Long taskId){return ApiResponse.ok(submissions.findByStudentAndTask(studentId,taskId));}
    @GetMapping("/submissions/student/{id}") public ApiResponse<List<Submission>> studentSubmissions(@PathVariable Long id){return ApiResponse.ok(submissions.findByStudentId(id));}
    @GetMapping("/submissions/course/{id}") public ApiResponse<List<Submission>> courseSubmissions(@PathVariable Long id){return ApiResponse.ok(submissions.findByCourseId(id));}
    @GetMapping("/submissions/count") public ApiResponse<Integer> submissionCount(){return ApiResponse.ok(submissions.countAll());}
    @PostMapping("/submissions") public ApiResponse<Submission> insertSubmission(@RequestBody Submission value){submissions.insert(value);return ApiResponse.ok(value);}
    @PutMapping("/submissions/{id}") public ApiResponse<Integer> updateSubmission(@PathVariable Long id,@RequestBody Submission value){value.setId(id);return ApiResponse.ok(submissions.updateContent(value));}
    @PutMapping("/submissions/{id}/grade") public ApiResponse<Integer> grade(@PathVariable Long id,@RequestBody Submission value){value.setId(id);return ApiResponse.ok(submissions.grade(value));}

    @GetMapping("/exams/{id}") public ApiResponse<ExamRecord> exam(@PathVariable Long id){return ApiResponse.ok(exams.findById(id));}
    @GetMapping("/exams/student-task") public ApiResponse<ExamRecord> studentExam(@RequestParam Long studentId,@RequestParam Long taskId){return ApiResponse.ok(exams.findByStudentAndTask(studentId,taskId));}
    @GetMapping("/exams/task/{id}") public ApiResponse<List<ExamRecord>> taskExams(@PathVariable Long id){return ApiResponse.ok(exams.findByTaskId(id));}
    @GetMapping("/exams/student/{id}") public ApiResponse<List<ExamRecord>> studentExams(@PathVariable Long id){return ApiResponse.ok(exams.findByStudentId(id));}
    @PostMapping("/exams") public ApiResponse<ExamRecord> insertExam(@RequestBody ExamRecord value){exams.insert(value);return ApiResponse.ok(value);}
    @PutMapping("/exams/{id}/content") public ApiResponse<Integer> examContent(@PathVariable Long id,@RequestBody ExamRecord value){value.setId(id);return ApiResponse.ok(exams.updateContent(value));}
    @PutMapping("/exams/{id}/begin") public ApiResponse<Integer> examBegin(@PathVariable Long id,@RequestBody ExamRecord value){value.setId(id);return ApiResponse.ok(exams.beginExam(value));}
    @PutMapping("/exams/{id}/submit") public ApiResponse<Integer> examSubmit(@PathVariable Long id,@RequestBody ExamRecord value){value.setId(id);return ApiResponse.ok(exams.submit(value));}
    @PutMapping("/exams/{id}/auto-submit") public ApiResponse<Integer> examAutoSubmit(@PathVariable Long id,@RequestBody ExamRecord value){value.setId(id);return ApiResponse.ok(exams.autoSubmit(value));}
}
