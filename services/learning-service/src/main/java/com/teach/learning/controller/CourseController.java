package com.teach.learning.controller;
import com.teach.learning.dto.ApiResponse;
import com.teach.learning.dto.CourseView;
import com.teach.learning.entity.Course;
import com.teach.learning.service.CourseService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;
    public CourseController(CourseService courseService) { this.courseService = courseService; }

    @GetMapping("/{id}")
    public ApiResponse<CourseView> getById(@PathVariable Long id) {
        Course course = courseService.findById(id);
        if (course == null) return ApiResponse.fail(404, "课程不存在");
        return ApiResponse.ok(CourseView.from(course));
    }

    @GetMapping
    public ApiResponse<List<CourseView>> list(@RequestParam(required = false) Long teacherId,
                                               @RequestParam(required = false) Long studentId) {
        List<Course> courses;
        if (teacherId != null) courses = courseService.getTeacherCourses(teacherId);
        else if (studentId != null) courses = courseService.getStudentCourses(studentId);
        else return ApiResponse.fail("请提供 teacherId 或 studentId");
        return ApiResponse.ok(courses.stream().map(CourseView::from).collect(Collectors.toList()));
    }

    @PostMapping
    public ApiResponse<CourseView> create(@RequestParam Long teacherId, @RequestParam String name,
                                           @RequestParam(required = false) String code,
                                           @RequestParam(required = false) String description,
                                           @RequestParam(required = false) Integer credits,
                                           @RequestParam(required = false) String subjectCategory,
                                           @RequestParam(required = false) Integer hours,
                                           @RequestParam(required = false, defaultValue = "true") Boolean allowJoin,
                                           @RequestParam(required = false, defaultValue = "active") String status) {
        Course course = courseService.create(teacherId, name, code != null ? code : "", description,
            credits != null ? credits : 0, subjectCategory != null ? subjectCategory : "",
            hours != null ? hours : 0, allowJoin, status);
        return ApiResponse.ok(CourseView.from(course));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @RequestParam Long teacherId,
                                  @RequestParam String name, @RequestParam(required = false) String code,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(required = false) Integer credits,
                                  @RequestParam(required = false) String subjectCategory,
                                  @RequestParam(required = false) Integer hours,
                                  @RequestParam(required = false) Boolean allowJoin,
                                  @RequestParam(required = false) String status) {
        boolean ok = courseService.update(id, teacherId, name, code != null ? code : "",
            description, credits != null ? credits : 0, subjectCategory != null ? subjectCategory : "",
            hours != null ? hours : 0, allowJoin, status);
        return ok ? ApiResponse.ok("更新成功") : ApiResponse.fail(403, "无权限或课程不存在");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id, @RequestParam Long teacherId) {
        boolean ok = courseService.archive(id, teacherId);
        return ok ? ApiResponse.ok("归档成功") : ApiResponse.fail(403, "无权限或课程不存在");
    }
}
