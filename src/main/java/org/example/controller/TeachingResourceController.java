package org.example.controller;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.example.entity.Course;
import org.example.entity.TeachingResource;
import org.example.entity.User;
import org.example.mapper.CourseEnrollmentMapper;
import org.example.mapper.ResourceProgressMapper;
import org.example.mapper.TeachingResourceMapper;
import org.example.dto.ApiResponse;
import org.example.entity.ResourceProgress;
import org.example.bff.MicroserviceClient;
import org.springframework.beans.factory.annotation.Value;
import org.example.service.CourseService;
import org.example.util.DownloadUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpSession;
import java.nio.file.Paths;

@Controller
@RequestMapping
public class TeachingResourceController {

    private final CourseService courseService;
    private final TeachingResourceMapper resourceMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final ResourceProgressMapper progressMapper;
    private final MicroserviceClient microservices;
    private final boolean bffEnabled;

    public TeachingResourceController(CourseService courseService,
                                      TeachingResourceMapper resourceMapper,
                                      CourseEnrollmentMapper enrollmentMapper,
                                      ResourceProgressMapper progressMapper,
                                      MicroserviceClient microservices,
                                      @Value("${app.bff.enabled:false}") boolean bffEnabled) {
        this.courseService = courseService;
        this.resourceMapper = resourceMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.progressMapper = progressMapper;
        this.microservices = microservices;
        this.bffEnabled = bffEnabled;
    }

    @GetMapping("/teacher/resource/manage/{courseId}")
    public String manageResources(@PathVariable Long courseId, HttpSession session, Model model) {
        return manageResources(courseId, null, null, session, model);
    }

    @GetMapping("/teacher/resource/manage/{courseId}/filter")
    public String manageResources(@PathVariable Long courseId,
                                  @RequestParam(required = false) String type,
                                  @RequestParam(required = false) String chapter,
                                  HttpSession session,
                                  Model model) {
        User user = requireRole(session, "teacher");
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course == null) return "redirect:/teacher/course/manage";
        model.addAttribute("user", user);
        model.addAttribute("course", UserController.toCourseView(course));
        model.addAttribute("resources", resourceMapper.searchByCourse(courseId, type, chapter));
        model.addAttribute("filterType", type);
        model.addAttribute("filterChapter", chapter);
        return "teacher/resource_manage";
    }

    @PostMapping("/teacher/resource/upload")
    public String uploadResource(@RequestParam Long courseId,
                                 @RequestParam String title,
                                 @RequestParam(required = false) String chapter,
                                 @RequestParam("file") MultipartFile[] files,
                                 HttpSession session,
                                 Model model) {
        User user = requireRole(session, "teacher");
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course == null) return "redirect:/teacher/course/manage";
        if (files == null || files.length == 0) {
            model.addAttribute("user", user);
            model.addAttribute("course", UserController.toCourseView(course));
            model.addAttribute("resources", resourceMapper.findByCourseId(courseId));
            model.addAttribute("error", "请选择要上传的文件。");
            return "teacher/resource_manage";
        }
        for (MultipartFile file : files) {
            String type = detectType(file);
            if (type == null) continue;
            String path = saveFile(courseId, file);
            if (path == null) continue;
            TeachingResource resource = new TeachingResource();
            resource.setCourseId(courseId);
            resource.setTitle(title == null || title.trim().isEmpty() ? originalName(file) : title.trim());
            resource.setType(type);
            resource.setFilePath(path);
            resource.setChapter(chapter == null || chapter.trim().isEmpty() ? "默认章节" : chapter.trim());
            resource.setFileSize(file.getSize());
            resource.setDownloadCount(0);
            resourceMapper.insert(resource);
        }
        return "redirect:/teacher/resource/manage/" + courseId;
    }

    @PostMapping("/teacher/resource/update")
    public String updateResource(@RequestParam Long resourceId,
                                 @RequestParam String title,
                                 @RequestParam(required = false) String chapter,
                                 @RequestParam(required = false) String type,
                                 HttpSession session) {
        User user = requireRole(session, "teacher");
        if (user == null) return "redirect:/login";
        TeachingResource resource = resourceMapper.findById(resourceId);
        if (resource == null) return "redirect:/teacher/course/manage";
        Course course = ownedCourse(user, resource.getCourseId());
        if (course == null) return "redirect:/teacher/course/manage";
        resource.setTitle(title == null || title.trim().isEmpty() ? resource.getTitle() : title.trim());
        resource.setChapter(chapter == null || chapter.trim().isEmpty() ? "默认章节" : chapter.trim());
        resource.setType(type == null || type.trim().isEmpty() ? resource.getType() : type.trim());
        resourceMapper.updateMeta(resource);
        return "redirect:/teacher/resource/manage/" + resource.getCourseId();
    }

    @GetMapping("/teacher/resource/upload")
    public String uploadEntry(@RequestParam(required = false) Long courseId) {
        return courseId == null ? "redirect:/teacher/course/manage" : "redirect:/teacher/resource/manage/" + courseId;
    }

    @PostMapping("/teacher/resource/delete")
    public String deleteResource(@RequestParam Long resourceId, HttpSession session) {
        User user = requireRole(session, "teacher");
        if (user == null) return "redirect:/login";
        TeachingResource resource = resourceMapper.findById(resourceId);
        if (resource == null) return "redirect:/teacher/course/manage";
        Course course = ownedCourse(user, resource.getCourseId());
        if (course == null) return "redirect:/teacher/course/manage";
        resourceMapper.deleteById(resourceId);
        return "redirect:/teacher/resource/manage/" + resource.getCourseId();
    }

    @PostMapping("/teacher/resource/batch-delete")
    public String batchDeleteResource(@RequestParam Long courseId,
                                      @RequestParam(required = false) java.util.List<Long> resourceIds,
                                      HttpSession session) {
        User user = requireRole(session, "teacher");
        if (user == null) return "redirect:/login";
        Course course = ownedCourse(user, courseId);
        if (course != null && resourceIds != null) {
            for (Long id : resourceIds) {
                TeachingResource resource = resourceMapper.findById(id);
                if (resource != null && courseId.equals(resource.getCourseId())) {
                    resourceMapper.deleteById(id);
                }
            }
        }
        return "redirect:/teacher/resource/manage/" + courseId;
    }

    @GetMapping("/student/resource/download/{resourceId}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long resourceId, HttpSession session) throws Exception {
        User user = requireRole(session, "student");
        TeachingResource resource = accessibleStudentResource(user, resourceId);
        if (resource == null || !resource.isPdf()) {
            return ResponseEntity.status(403).build();
        }
        resourceMapper.incrementDownloadCount(resourceId);
        return bffEnabled ? remoteFile(resource.getFilePath(), false) : DownloadUtils.attachment(resource.getFilePath());
    }

    @GetMapping("/resource-square/download/{resourceId}")
    public ResponseEntity<Resource> downloadSquareResource(@PathVariable Long resourceId, HttpSession session) throws Exception {
        TeachingResource resource = resourceMapper.findById(resourceId);
        if (resource == null) return ResponseEntity.notFound().build();
        resourceMapper.incrementDownloadCount(resourceId);
        return bffEnabled ? remoteFile(resource.getFilePath(), false) : DownloadUtils.attachment(resource.getFilePath());
    }

    @GetMapping("/student/resource/video/{resourceId}")
    public String videoPage(@PathVariable Long resourceId, HttpSession session, Model model) {
        User user = requireRole(session, "student");
        TeachingResource resource = accessibleStudentResource(user, resourceId);
        if (resource == null || !resource.isVideo()) return "redirect:/student/course/my";
        model.addAttribute("user", user);
        model.addAttribute("resource", resource);
        model.addAttribute("course", UserController.toCourseView(courseService.findById(resource.getCourseId())));
        return "student/resource_video";
    }

    @GetMapping("/student/resource/stream/{resourceId}")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long resourceId, HttpSession session) throws Exception {
        User user = requireRole(session, "student");
        TeachingResource resource = accessibleStudentResource(user, resourceId);
        if (resource == null || !resource.isVideo()) {
            return ResponseEntity.status(403).build();
        }
        if (bffEnabled) return remoteFile(resource.getFilePath(), true);
        return DownloadUtils.inline(resource.getFilePath(), videoType(DownloadUtils.resolvePath(resource.getFilePath()).getFileName().toString()));
    }

    @GetMapping("/student/resource/notes/{resourceId}")
    public String pdfNotes(@PathVariable Long resourceId, HttpSession session, Model model) {
        User user = requireRole(session, "student");
        TeachingResource resource = accessibleStudentResource(user, resourceId);
        if (resource == null || !resource.isPdf()) return "redirect:/student/course/my";
        model.addAttribute("user", user);
        model.addAttribute("resource", resource);
        model.addAttribute("course", UserController.toCourseView(courseService.findById(resource.getCourseId())));
        model.addAttribute("notes", generatePdfNotes(resource));
        return "student/resource_notes";
    }

    @PostMapping("/student/resource/progress")
    @ResponseBody
    public ApiResponse<String> updateProgress(@RequestBody java.util.Map<String, Object> body, HttpSession session) {
        User user = requireRole(session, "student");
        if (user == null) return ApiResponse.fail(401, "请先登录");
        Long resourceId = toLong(body.get("resourceId"));
        if (resourceId == null) return ApiResponse.fail("资源不存在");
        TeachingResource resource = accessibleStudentResource(user, resourceId);
        if (resource == null || !resource.isVideo()) return ApiResponse.fail(403, "无权访问该资源");
        double current = toDouble(body.get("currentTime"));
        double duration = toDouble(body.get("duration"));
        double percent = duration <= 0 ? 0 : Math.max(0, Math.min(100, current * 100.0 / duration));
        ResourceProgress progress = progressMapper.find(user.getId(), resourceId);
        if (progress == null) {
            progress = new ResourceProgress();
            progress.setStudentId(user.getId());
            progress.setResourceId(resourceId);
            progress.setProgress(percent);
            progress.setLastPosition(current);
            progress.setDuration(duration);
            progressMapper.insert(progress);
        } else if (percent >= (progress.getProgress() == null ? 0 : progress.getProgress()) || current > (progress.getLastPosition() == null ? 0 : progress.getLastPosition())) {
            progress.setProgress(Math.max(percent, progress.getProgress() == null ? 0 : progress.getProgress()));
            progress.setLastPosition(current);
            progress.setDuration(duration);
            progressMapper.update(progress);
        }
        return ApiResponse.ok("学习进度已更新");
    }

    private User requireRole(HttpSession session, String role) {
        User user = UserController.requireUser(session);
        return user != null && role.equals(user.getRole()) ? user : null;
    }

    private Course ownedCourse(User user, Long courseId) {
        Course course = courseService.findById(courseId);
        return course != null && user.getId().equals(course.getTeacherId()) ? course : null;
    }

    private TeachingResource accessibleStudentResource(User user, Long resourceId) {
        if (user == null) return null;
        TeachingResource resource = resourceMapper.findById(resourceId);
        if (resource == null) return null;
        return enrollmentMapper.findByStudentAndCourse(user.getId(), resource.getCourseId()) == null ? null : resource;
    }

    private String saveFile(Long courseId, MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            if (bffEnabled) {
                org.springframework.util.MultiValueMap<String,Object> fields = new org.springframework.util.LinkedMultiValueMap<>();
                fields.add("courseId", String.valueOf(courseId));
                return microservices.upload(microservices.learning("/internal/files/resources"), file, "file", fields);
            }
            return DownloadUtils.store(file, "resources", String.valueOf(courseId));
        } catch (Exception e) {
            return null;
        }
    }

    private String detectType(MultipartFile file) {
        String filename = originalName(file).toLowerCase();
        String contentType = file == null ? "" : String.valueOf(file.getContentType()).toLowerCase();
        if (filename.endsWith(".pdf") || contentType.contains("pdf")) return "pdf";
        if (filename.endsWith(".ppt") || filename.endsWith(".pptx") || contentType.contains("presentation")) return "ppt";
        if (filename.endsWith(".zip") || filename.endsWith(".rar") || filename.endsWith(".7z") || filename.endsWith(".tar.gz")) return "code";
        if (filename.endsWith(".mp4") || filename.endsWith(".webm") || filename.endsWith(".mov") || contentType.startsWith("video/")) {
            return "video";
        }
        return null;
    }

    private String originalName(MultipartFile file) {
        String filename = file == null ? "" : file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) return "resource";
        return Paths.get(filename).getFileName().toString();
    }

    private MediaType videoType(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".webm")) return MediaType.parseMediaType("video/webm");
        if (lower.endsWith(".mov")) return MediaType.parseMediaType("video/quicktime");
        return MediaType.parseMediaType("video/mp4");
    }

    private String generatePdfNotes(TeachingResource resource) {
        try (PDDocument document = bffEnabled
                ? PDDocument.load(microservices.file(microservices.uri(microservices.learning("/internal/files")).queryParam("path", resource.getFilePath()).toUriString()).getBody())
                : PDDocument.load(DownloadUtils.resolvePath(resource.getFilePath()).toFile())) {
            String text = new PDFTextStripper().getText(document);
            java.util.Map<String, String> request = new java.util.LinkedHashMap<>();
            request.put("courseName", resource.getCourseName());
            request.put("resourceTitle", resource.getTitle());
            request.put("text", text);
            return microservices.post(microservices.learning("/api/v2/ai/summarize"), request, String.class);
        } catch (Exception e) {
            return "PDF 读取或 AI 笔记生成失败：" + e.getMessage();
        }
    }

    private ResponseEntity<Resource> remoteFile(String filePath, boolean inline) {
        String url = microservices.uri(microservices.learning("/internal/files")).queryParam("path", filePath).toUriString();
        ResponseEntity<byte[]> response = microservices.file(url);
        byte[] body = response.getBody();
        if (body == null || body.length == 0) return ResponseEntity.status(response.getStatusCode()).build();
        ByteArrayResource resource = new ByteArrayResource(body);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.getStatusCode());
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType != null) builder.contentType(contentType);
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (disposition != null) builder.header(HttpHeaders.CONTENT_DISPOSITION, disposition);
        return builder.body(resource);
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return value == null ? 0 : Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }
}
