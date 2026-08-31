package com.teach.learning.controller.internal;
import com.teach.learning.dto.ApiResponse;
import com.teach.learning.dto.CourseView;
import com.teach.learning.entity.Course;
import com.teach.learning.service.CourseService;
import com.teach.learning.service.EnrollmentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/courses")
public class InternalCourseController {
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    public InternalCourseController(CourseService courseService, EnrollmentService enrollmentService) { this.courseService = courseService; this.enrollmentService = enrollmentService; }

    @GetMapping("/{id}")
    public ApiResponse<CourseView> byId(@PathVariable Long id) {
        Course course = courseService.findById(id);
        if (course == null) return ApiResponse.fail(404, "课程不存在");
        return ApiResponse.ok(CourseView.from(course));
    }

    @GetMapping("/{id}/access")
    public ApiResponse<Boolean> checkAccess(@PathVariable Long id, @RequestParam Long userId) {
        Course course = courseService.findById(id);
        if (course == null) return ApiResponse.fail(404, "课程不存在");
        return ApiResponse.ok(courseService.canAccess(id, userId, "view") || enrollmentService.isEnrolled(userId, id));
    }

    @GetMapping("/{id}/authorization")
    public ApiResponse<Boolean> authorization(@PathVariable Long id, @RequestParam Long userId, @RequestParam String action) {
        if (courseService.findById(id) == null) return ApiResponse.fail(404, "课程不存在");
        boolean allowed = courseService.canAccess(id, userId, action);
        if ("view".equalsIgnoreCase(action) || "submit".equalsIgnoreCase(action) || "exam".equalsIgnoreCase(action)) {
            allowed = enrollmentService.isEnrolled(userId, id) || allowed && courseService.findById(id).getTeacherId().equals(userId);
        }
        return ApiResponse.ok(allowed);
    }

    @GetMapping("/active")
    public ApiResponse<List<CourseView>> active() {
        return ApiResponse.ok(courseService.getActiveCourses().stream().map(CourseView::from).collect(Collectors.toList()));
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
}
