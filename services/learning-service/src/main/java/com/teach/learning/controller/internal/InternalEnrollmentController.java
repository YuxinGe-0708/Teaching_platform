package com.teach.learning.controller.internal;
import com.teach.learning.dto.ApiResponse;
import com.teach.learning.dto.EnrollmentView;
import com.teach.learning.entity.CourseEnrollment;
import com.teach.learning.entity.CourseClass;
import com.teach.learning.service.EnrollmentService;
import com.teach.learning.service.CourseClassService;
import com.teach.learning.service.CourseService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/enrollments")
public class InternalEnrollmentController {
    private final EnrollmentService enrollmentService;
    private final CourseClassService classService; private final CourseService courseService;
    public InternalEnrollmentController(EnrollmentService enrollmentService, CourseClassService classService, CourseService courseService) { this.enrollmentService = enrollmentService; this.classService=classService; this.courseService=courseService; }

    @GetMapping("/check")
    public ApiResponse<Boolean> check(@RequestParam Long studentId, @RequestParam Long courseId) {
        return ApiResponse.ok(enrollmentService.isEnrolled(studentId, courseId));
    }

    @GetMapping("/student/{studentId}")
    public ApiResponse<List<EnrollmentView>> byStudent(@PathVariable Long studentId) {
        return ApiResponse.ok(enrollmentService.getStudentEnrollments(studentId).stream().map(EnrollmentView::from).collect(Collectors.toList()));
    }

    @GetMapping("/course/{courseId}")
    public ApiResponse<List<EnrollmentView>> byCourse(@PathVariable Long courseId) {
        return ApiResponse.ok(enrollmentService.getCourseEnrollments(courseId).stream().map(EnrollmentView::from).collect(Collectors.toList()));
    }

    @PostMapping
    public ApiResponse<EnrollmentView> enroll(@RequestParam Long studentId, @RequestParam Long courseId,
                                               @RequestParam(required = false) Long classId) {
        CourseEnrollment e = enrollmentService.enroll(studentId, courseId, classId);
        if (e == null) return ApiResponse.fail("选课失败");
        return ApiResponse.ok(EnrollmentView.from(e));
    }

    @DeleteMapping
    public ApiResponse<?> unenroll(@RequestParam Long studentId, @RequestParam Long courseId) {
        return enrollmentService.unenroll(studentId, courseId) ? ApiResponse.ok("退课成功") : ApiResponse.fail("未选该课程");
    }

    @DeleteMapping("/{studentId}/{courseId}")
    public ApiResponse<?> unenrollPath(@PathVariable Long studentId, @PathVariable Long courseId) {
        return unenroll(studentId, courseId);
    }

    @PostMapping("/by-invite")
    public ApiResponse<EnrollmentView> enrollByInvite(@RequestParam Long studentId, @RequestParam String inviteCode,
                                                       @RequestParam(required = false) Long classId) {
        CourseClass cc = classService.findByInviteCode(inviteCode);
        Long courseId = cc == null ? null : cc.getCourseId();
        if (courseId == null) {
            com.teach.learning.entity.Course course = courseService.findByInviteCode(inviteCode);
            courseId = course == null ? null : course.getId();
        }
        if (courseId == null) return ApiResponse.fail(404, "邀请码无效");
        CourseEnrollment e = enrollmentService.enroll(studentId, courseId, classId != null ? classId : (cc == null ? null : cc.getId()));
        return e == null ? ApiResponse.fail("选课失败") : ApiResponse.ok(EnrollmentView.from(e));
    }
}
