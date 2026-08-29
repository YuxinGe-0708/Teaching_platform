package com.teach.learning.controller;
import com.teach.learning.dto.ApiResponse;
import com.teach.learning.entity.StudyNote;
import com.teach.learning.service.StudyNoteService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/study-notes")
public class StudyNoteController {
    private final StudyNoteService noteService;
    public StudyNoteController(StudyNoteService noteService) { this.noteService = noteService; }

    private Map<String,Object> toView(StudyNote n) {
        Map<String,Object> m = new HashMap<>();
        m.put("id", n.getId()); m.put("studentId", n.getStudentId()); m.put("courseId", n.getCourseId());
        m.put("resourceId", n.getResourceId()); m.put("title", n.getTitle()); m.put("content", n.getContent());
        m.put("aiSummary", n.getAiSummary()); m.put("mindMap", n.getMindMap());
        m.put("createdAt", n.getCreatedAt()); m.put("updatedAt", n.getUpdatedAt()); return m;
    }

    @GetMapping("/student/{studentId}")
    public ApiResponse<List<Map<String,Object>>> list(@PathVariable Long studentId, @RequestParam(required = false) Long courseId) {
        List<StudyNote> notes = courseId != null ? noteService.findByStudentAndCourse(studentId, courseId) : noteService.findByStudentId(studentId);
        return ApiResponse.ok(notes.stream().map(this::toView).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String,Object>> getById(@PathVariable Long id) {
        StudyNote note = noteService.findById(id);
        if (note == null) return ApiResponse.fail(404, "笔记不存在");
        return ApiResponse.ok(toView(note));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestParam Long studentId, @RequestParam Long courseId,
                                  @RequestParam(required = false) Long resourceId,
                                  @RequestParam String title, @RequestParam String content) {
        StudyNote note = noteService.create(studentId, courseId, resourceId, title, content);
        return ApiResponse.ok("创建成功, id=" + note.getId());
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @RequestParam String title, @RequestParam String content,
                                  @RequestParam(required = false) String aiSummary,
                                  @RequestParam(required = false) String mindMap) {
        return noteService.update(id, title, content, aiSummary, mindMap) ? ApiResponse.ok("更新成功") : ApiResponse.fail(404, "笔记不存在");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        return noteService.delete(id) ? ApiResponse.ok("删除成功") : ApiResponse.fail(404, "笔记不存在");
    }
}
