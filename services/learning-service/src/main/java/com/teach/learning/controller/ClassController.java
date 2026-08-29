package com.teach.learning.controller;
import com.teach.learning.dto.ApiResponse;
import com.teach.learning.dto.ClassView;
import com.teach.learning.entity.CourseClass;
import com.teach.learning.service.CourseClassService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classes")
public class ClassController {
    private final CourseClassService classService;
    public ClassController(CourseClassService classService) { this.classService = classService; }

    @GetMapping("/{id}")
    public ApiResponse<ClassView> getById(@PathVariable Long id) {
        CourseClass cc = classService.findById(id);
        if (cc == null) return ApiResponse.fail(404, "班级不存在");
        return ApiResponse.ok(ClassView.from(cc));
    }

    @GetMapping
    public ApiResponse<List<ClassView>> list(@RequestParam Long courseId) {
        return ApiResponse.ok(classService.findByCourseId(courseId).stream().map(ClassView::from).collect(Collectors.toList()));
    }

    @PostMapping
    public ApiResponse<ClassView> create(@RequestParam Long courseId, @RequestParam(required = false) String name,
                                          @RequestParam(required = false, defaultValue = "100") Integer maxCount) {
        return ApiResponse.ok(ClassView.from(classService.create(courseId, name, maxCount)));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @RequestParam(required = false) String name,
                                  @RequestParam(required = false) Integer maxCount) {
        return classService.update(id, name, maxCount) ? ApiResponse.ok("更新成功") : ApiResponse.fail(404, "班级不存在");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        return classService.delete(id) ? ApiResponse.ok("删除成功") : ApiResponse.fail(404, "班级不存在");
    }
}
