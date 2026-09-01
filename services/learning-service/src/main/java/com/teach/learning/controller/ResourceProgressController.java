package com.teach.learning.controller;
import com.teach.learning.dto.ApiResponse;
import com.teach.learning.entity.ResourceProgress;
import com.teach.learning.service.ResourceProgressService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/resource-progress")
public class ResourceProgressController {
    private final ResourceProgressService progressService;
    public ResourceProgressController(ResourceProgressService progressService) { this.progressService = progressService; }

    @GetMapping
    public ApiResponse<Map<String,Object>> get(@RequestParam Long studentId, @RequestParam Long resourceId) {
        ResourceProgress rp = progressService.findByStudentAndResource(studentId, resourceId);
        Map<String,Object> m = new HashMap<>();
        if (rp == null) { m.put("progress", 0.0); m.put("lastPosition", 0.0); m.put("duration", 0.0); }
        else { m.put("progress", rp.getProgress()); m.put("lastPosition", rp.getLastPosition()); m.put("duration", rp.getDuration()); }
        return ApiResponse.ok(m);
    }

    @PostMapping
    public ApiResponse<?> save(@RequestParam Long studentId, @RequestParam Long resourceId,
                                @RequestParam(required = false, defaultValue = "0") Double progress,
                                @RequestParam(required = false, defaultValue = "0") Double lastPosition,
                                @RequestParam(required = false, defaultValue = "0") Double duration) {
        progressService.save(studentId, resourceId, progress, lastPosition, duration);
        return ApiResponse.ok("保存成功");
    }
}
