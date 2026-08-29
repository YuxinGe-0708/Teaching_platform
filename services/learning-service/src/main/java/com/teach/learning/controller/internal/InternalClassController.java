package com.teach.learning.controller.internal;
import com.teach.learning.dto.ApiResponse;
import com.teach.learning.dto.ClassView;
import com.teach.learning.entity.CourseClass;
import com.teach.learning.service.CourseClassService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/classes")
public class InternalClassController {
    private final CourseClassService classService;
    public InternalClassController(CourseClassService classService) { this.classService = classService; }

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
}
