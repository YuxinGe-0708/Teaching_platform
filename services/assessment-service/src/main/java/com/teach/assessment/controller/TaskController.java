package com.teach.assessment.controller;

import com.teach.assessment.dto.ApiResponse;
import com.teach.assessment.entity.Task;
import com.teach.assessment.service.TaskService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/internal/tasks")
public class TaskController {
    private final TaskService service;
    private final com.teach.assessment.service.LearningServiceClient learning;
    public TaskController(TaskService service, com.teach.assessment.service.LearningServiceClient learning) { this.service = service; this.learning = learning; }
    @GetMapping("/{id}") public ApiResponse<Task> get(@PathVariable Long id) {
        Task t = service.findById(id); return t == null ? ApiResponse.fail(404,"任务不存在") : ApiResponse.ok(t);
    }
    @GetMapping public ApiResponse<List<Task>> list(@RequestParam(required=false) Long courseId, @RequestParam(required=false) Long studentId) {
        if (courseId != null) return ApiResponse.ok(service.getCourseTasks(courseId));
        if (studentId != null) { java.util.Set<Long> courses=learning.enrolledCourseIds(studentId); java.util.List<Task> result=new java.util.ArrayList<>(); for(Task t:service.getPublishedTasks()) if(courses.contains(t.getCourseId())) result.add(t); return ApiResponse.ok(result); }
        return ApiResponse.fail("请提供 courseId 或 studentId");
    }
    @GetMapping("/student/{studentId}") public ApiResponse<List<Task>> studentTasks(@PathVariable Long studentId) { return list(null, studentId); }
    @PostMapping public ApiResponse<Task> create(@RequestBody Task task) {
        if (task.getCourseId() == null || !learning.courseExists(task.getCourseId())) return ApiResponse.fail(400, "课程不存在或学习服务不可用");
        return ApiResponse.ok(service.createTask(task));
    }
    @PutMapping("/{id}") public ApiResponse<Task> update(@PathVariable Long id, @RequestBody Task task) {
        task.setId(id); return ApiResponse.ok(service.updateTask(task));
    }
    @PatchMapping("/{id}/status") public ApiResponse<String> status(@PathVariable Long id, @RequestParam String status) {
        service.updateStatus(id,status); return ApiResponse.ok("ok",null);
    }
}
