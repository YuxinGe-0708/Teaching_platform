package com.teach.learning.controller.internal;
import com.teach.learning.dto.ApiResponse;
import com.teach.learning.dto.ClassView;
import com.teach.learning.entity.CourseClass;
import com.teach.learning.service.CourseClassService;
import com.teach.learning.service.EnrollmentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/classes")
public class InternalClassController {
    private final CourseClassService classService; private final EnrollmentService enrollmentService;
    public InternalClassController(CourseClassService classService, EnrollmentService enrollmentService) { this.classService = classService; this.enrollmentService = enrollmentService; }

    @GetMapping("/{id}")
    public ApiResponse<ClassView> byId(@PathVariable Long id) {
        CourseClass cc = classService.findById(id);
        if (cc == null) return ApiResponse.fail(404, "班级不存在");
        return ApiResponse.ok(ClassView.from(cc));
    }

    @GetMapping
    public ApiResponse<List<ClassView>> byCourse(@RequestParam Long courseId) {
        return ApiResponse.ok(classService.findByCourseId(courseId).stream().map(ClassView::from).collect(Collectors.toList()));
    }

    @GetMapping("/course/{courseId}")
    public ApiResponse<List<ClassView>> byCoursePath(@PathVariable Long courseId) { return byCourse(courseId); }

    @PostMapping
    public ApiResponse<ClassView> create(@RequestParam Long courseId, @RequestParam(required=false) String name,
                                         @RequestParam(required=false) Integer maxCount) {
        return ApiResponse.ok(ClassView.from(classService.create(courseId, name, maxCount)));
    }

    @PutMapping("/{id}")
    public ApiResponse<String> update(@PathVariable Long id, @RequestParam String name, @RequestParam Integer maxCount) {
        return classService.update(id, name, maxCount) ? ApiResponse.ok("ok", null) : ApiResponse.fail(404, "班级不存在");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) { return classService.delete(id) ? ApiResponse.ok("ok",null) : ApiResponse.fail(404,"班级不存在"); }

    @DeleteMapping("/{id}/members/{studentId}")
    public ApiResponse<String> removeMember(@PathVariable Long id, @PathVariable Long studentId) {
        return enrollmentService.removeFromClass(id, studentId) ? ApiResponse.ok("ok",null) : ApiResponse.fail(404,"班级成员不存在");
    }
}
