package com.teach.learning.controller;
import com.teach.learning.dto.ApiResponse;
import com.teach.learning.entity.Resource;
import com.teach.learning.service.ResourceService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {
    private final ResourceService resourceService;
    public ResourceController(ResourceService resourceService) { this.resourceService = resourceService; }

    @GetMapping("/{id}")
    public ApiResponse<Map<String,Object>> getById(@PathVariable Long id) {
        Resource r = resourceService.findById(id);
        if (r == null) return ApiResponse.fail(404, "资源不存在");
        resourceService.incrementDownloadCount(id);
        Map<String,Object> m = new HashMap<>();
        m.put("id", r.getId()); m.put("courseId", r.getCourseId()); m.put("title", r.getTitle());
        m.put("filePath", r.getFilePath()); m.put("type", r.getType()); m.put("chapter", r.getChapter());
        m.put("fileSize", r.getFileSize()); m.put("downloadCount", r.getDownloadCount() + 1);
        m.put("createdAt", r.getCreatedAt()); return ApiResponse.ok(m);
    }

    @GetMapping
    public ApiResponse<List<Map<String,Object>>> list(@RequestParam Long courseId) {
        return ApiResponse.ok(resourceService.findByCourseId(courseId).stream().map(r -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", r.getId()); m.put("courseId", r.getCourseId()); m.put("title", r.getTitle());
            m.put("filePath", r.getFilePath()); m.put("type", r.getType()); m.put("chapter", r.getChapter());
            m.put("fileSize", r.getFileSize()); m.put("downloadCount", r.getDownloadCount());
            m.put("createdAt", r.getCreatedAt()); return m;
        }).collect(Collectors.toList()));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestParam Long courseId, @RequestParam String title,
                                  @RequestParam(required = false) String filePath,
                                  @RequestParam(required = false) String type,
                                  @RequestParam(required = false) String chapter,
                                  @RequestParam(required = false) Long fileSize) {
        Resource r = resourceService.create(courseId, title, filePath, type, chapter, fileSize);
        return ApiResponse.ok("创建成功, id=" + r.getId());
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @RequestParam String title,
                                  @RequestParam(required = false) String filePath,
                                  @RequestParam(required = false) String type,
                                  @RequestParam(required = false) String chapter) {
        return resourceService.update(id, title, filePath, type, chapter) ? ApiResponse.ok("更新成功") : ApiResponse.fail(404, "资源不存在");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        return resourceService.delete(id) ? ApiResponse.ok("删除成功") : ApiResponse.fail(404, "资源不存在");
    }
}
